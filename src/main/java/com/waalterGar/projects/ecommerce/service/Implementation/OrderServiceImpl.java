package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderItemDto;
import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.entity.OrderItem;
import com.waalterGar.projects.ecommerce.entity.Product;
import com.waalterGar.projects.ecommerce.mapper.OrderMapper;
import com.waalterGar.projects.ecommerce.repository.CustomerRepository;
import com.waalterGar.projects.ecommerce.repository.OrderRepository;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.service.OrderService;
import com.waalterGar.projects.ecommerce.service.exception.InsufficientStockException;
import com.waalterGar.projects.ecommerce.utils.Currency;
import com.waalterGar.projects.ecommerce.utils.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

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

        order.setTotalAmount(totalAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Override
    public OrderDto getOrderByExternalId(String externalId) {
        Order order = orderRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
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
