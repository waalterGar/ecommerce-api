package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;
import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.entity.OrderItem;
import com.waalterGar.projects.ecommerce.mapper.OrderMapper;
import com.waalterGar.projects.ecommerce.repository.CustomerRepository;
import com.waalterGar.projects.ecommerce.repository.OrderRepository;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.service.OrderService;
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

    @Override
    public OrderDto createOrder(createOrderDto orderDto) {
        System.out.println("Creating order : " + orderDto);
        Customer customer = customerRepository.findByExternalId(orderDto.getCustomerExternalId())
                .orElseThrow(() -> new NoSuchElementException("Order not found"));

        Order order = OrderMapper.fromCreateDto(orderDto);
        order.setCustomer(customer);
        order.setExternalId(UUID.randomUUID().toString());

        orderDto.getItems().forEach(it -> {
            var product = productRepository.findBySku(it.getProductSku())
                    .orElseThrow(() -> new NoSuchElementException("Product not found: " + it.getProductSku()));

            var item = new OrderItem();
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setQuantity(it.getQuantity());
            item.setUnitPrice(product.getPrice());
            item.setCurrency(product.getCurrency());
            item.computeLineTotal();

            order.addItem(item);
        });
        BigDecimal total = order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);

        return OrderMapper.toDto(orderRepository.save(order));
    }

    @Override
    public OrderDto getOrderByExternalId(String externalId) {
        System.out.println("Fetching order with externalId: " + externalId);
        Order order = orderRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        System.out.println("Found order with externalId: " + order.getExternalId()+" , customer: "+ order.getCustomer().getFirstName()+  ", totalAmount: " + order.getTotalAmount()+ ", items count: " + order.getItems().size());
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
