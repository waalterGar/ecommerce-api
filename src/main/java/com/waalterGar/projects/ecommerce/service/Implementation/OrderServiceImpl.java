package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.*;
import com.waalterGar.projects.ecommerce.entity.*;
import com.waalterGar.projects.ecommerce.mapper.OrderMapper;
import com.waalterGar.projects.ecommerce.repository.CustomerRepository;
import com.waalterGar.projects.ecommerce.repository.OrderRepository;
import com.waalterGar.projects.ecommerce.repository.PaymentRepository;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.service.OrderService;
import com.waalterGar.projects.ecommerce.service.exception.InactiveProductException;
import com.waalterGar.projects.ecommerce.service.exception.InsufficientStockException;
import com.waalterGar.projects.ecommerce.utils.Currency;
import com.waalterGar.projects.ecommerce.utils.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static java.math.RoundingMode.HALF_UP;

@Transactional
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    @Override
    public OrderDto createOrder(createOrderDto orderDto) {
        Customer customer = customerRepository.findByExternalId(orderDto.getCustomerExternalId())
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + orderDto.getCustomerExternalId()));

        Order order = OrderMapper.fromCreateDto(orderDto);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);
        order.setExternalId(UUID.randomUUID().toString());
        order.setTotalAmount(BigDecimal.ZERO);

        BigDecimal totalAmount = BigDecimal.ZERO;
        Currency orderCurrency = null;

        for (createOrderItemDto itm : orderDto.getItems()) {
            Product product = productRepository.findBySku(itm.getProductSku())
                    .orElseThrow(() -> new NoSuchElementException("Product not found: " + itm.getProductSku()));

            if (Boolean.FALSE.equals(product.getIsActive())) {
                throw new InactiveProductException("Product is inactive: " + product.getSku());
            }

            int requestedQuantity = itm.getQuantity();
            int availableStock = product.getStockQuantity();

            if (requestedQuantity > availableStock) {
                throw new InsufficientStockException(
                        "Insufficient stock for SKU " + product.getSku() +
                        " (requested " + requestedQuantity +
                        ", available " + availableStock + ")");
            }

            if (orderCurrency == null) {
                orderCurrency = product.getCurrency();
                order.setCurrency(orderCurrency);
            } else if (product.getCurrency() != orderCurrency) {
                throw new IllegalArgumentException("Currency mismatch for SKU " + product.getSku());
            }

            product.setStockQuantity(availableStock - requestedQuantity);

            OrderItem item = new OrderItem();
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setQuantity(itm.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setCurrency(product.getCurrency());
            item.computeLineTotal();

            order.addItem(item);
            totalAmount = totalAmount.add(item.getLineTotal());
        }

        order.setTotalAmount(totalAmount.setScale(2, HALF_UP));
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Override
    public OrderDto getOrderByExternalId(String externalId) {
        Order order = orderRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        return OrderMapper.toDto(order);
    }

    @Transactional
    @Override
    public OrderDto pay(String externalId, PayOrderRequestDto dto) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("Invalid externalId");
        }

        Order order = orderRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        if (order.getStatus() == OrderStatus.PAID) {
            return OrderMapper.toDto(order);
        }

        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalArgumentException("Order not payable from status " + order.getStatus());
        }

        if (dto != null) {
            if (dto.getAmount() != null) {
                BigDecimal expected = order.getTotalAmount().setScale(2, HALF_UP);
                BigDecimal provided = dto.getAmount().setScale(2, HALF_UP);
                if (expected.compareTo(provided) != 0) {
                    System.out.println("expected: "+ expected);
                    System.out.println("provided: "+ provided);
                    throw new IllegalArgumentException("Amount mismatch");
                }
            }

            if (dto.getCurrency() != null) {
                Currency provided = dto.getCurrency();
                if (provided != order.getCurrency()) {
                    throw new IllegalArgumentException("Currency mismatch");
                }
            }

            String txRef = dto != null ? dto.getTransactionReference() : null;

            if (txRef != null) {
                var existing = paymentRepository
                        .findByOrder_ExternalIdAndTransactionReference(externalId, txRef);
                if (existing.isPresent()) {
                    if (order.getStatus() != OrderStatus.PAID) {
                        order.setStatus(OrderStatus.PAID);
                        order.setPaidAt(existing.get().getPaidAt());
                    }
                    return OrderMapper.toDto(order);
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Payment p = new Payment();
        p.setOrder(order);
        p.setAmount(order.getTotalAmount().setScale(2, HALF_UP));
        p.setCurrency(order.getCurrency());

        if (dto != null) {
            p.setProvider(dto.getProvider());
            p.setTransactionReference(dto.getTransactionReference());
        }

        p.setPaidAt(now);
        paymentRepository.save(p);

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);

        return OrderMapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAllOrders() {
        return orderRepository.findAllWithItemsAndCustomer()
                .stream()
                .map(OrderMapper::toDto)
                .toList();

    }
}
