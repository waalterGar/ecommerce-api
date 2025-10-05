package com.waalterGar.projects.ecommerce.repository;

import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.testsupport.builders.CustomerBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.OrderBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.OrderItemBuilder;
import com.waalterGar.projects.ecommerce.utils.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryIT {

    @Container
    @ServiceConnection  // Boot wires spring.datasource.* from this container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.37");
    @Autowired OrderRepository orderRepository;
    @Autowired CustomerRepository customerRepository;

    private static final String CUSTOMER_EXT_ID = "cust-it-1";
    private static final String ORDER_EXT_ID = "ord-it-1";

    @Test
    void findAllWithItemsAndCustomer_returnsOrders_withItemsAndCustomerLoaded() {
        // Arrange
        Customer customer = new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build();
        Customer savedCustomer = customerRepository.save(customer);

        Order order = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withCustomer(savedCustomer)
                .addItem(new OrderItemBuilder()
                        .withSku("MUG-LOGO-001")
                        .withName("Logo Mug")
                        .withQuantity(2)
                        .withUnitPrice(new BigDecimal("19.99"))
                        .withCurrency(Currency.EUR)
                        .build())
                .build();
        //Set the bi-directional relationship
        order.getItems().forEach(item -> item.setOrder(order));

        orderRepository.save(order);

        // Act
        List<Order> result = orderRepository.findAllWithItemsAndCustomer();

        // Assert
        assertThat(result).hasSize(1);
        Order persistedOrder = result.get(0);
        assertThat(persistedOrder.getExternalId()).isEqualTo(ORDER_EXT_ID);
        // customer eagerly available
        assertThat(persistedOrder.getCustomer().getExternalId()).isEqualTo(CUSTOMER_EXT_ID);
        // items eagerly available
        assertThat(persistedOrder.getItems()).hasSize(1);
        assertThat(persistedOrder.getItems().get(0).getProductSku()).isEqualTo("MUG-LOGO-001");
        assertThat(persistedOrder.getItems().get(0).getQuantity()).isEqualTo(2);
    }
}