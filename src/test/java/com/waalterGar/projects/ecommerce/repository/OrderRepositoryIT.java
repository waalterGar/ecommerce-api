package com.waalterGar.projects.ecommerce.repository;

import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.entity.OrderItem;
import com.waalterGar.projects.ecommerce.testsupport.builders.CustomerBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.OrderBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.OrderItemBuilder;
import com.waalterGar.projects.ecommerce.utils.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.37");

    @Autowired OrderRepository orderRepository;
    @Autowired CustomerRepository customerRepository;

    private static final String CUSTOMER_EXT_ID    = "cust-it-1";
    private static final String CUSTOMER_EXT_ID_A  = "cust-A";
    private static final String ORDER_EXT_ID_1     = "ord-it-1";
    private static final String ORDER_EXT_ID_2     = "ord-it-2";
    private static final String SKU_MUG            = "MUG-LOGO-001";
    private static final String NAME_MUG           = "Logo Mug";
    private static final String SKU_TSHIRT         = "TSHIRT-LOGO-001";
    private static final String NAME_TSHIRT        = "Logo T-Shirt";
    private static final BigDecimal PRICE_MUG      = new BigDecimal("19.99");
    private static final BigDecimal PRICE_TSHIRT   = new BigDecimal("9.99");
    private static final int QTY_ONE               = 1;
    private static final int QTY_TWO               = 2;

    @Test
    void findAllWithItemsAndCustomer_returnsOrders_withItemsAndCustomerLoaded() {
        // Arrange
        Customer saved = savedCustomer(CUSTOMER_EXT_ID);
        Order order = orderWith(saved, ORDER_EXT_ID_1,
                item(SKU_MUG, NAME_MUG, QTY_TWO, PRICE_MUG));

        orderRepository.save(order);

        // Act
        List<Order> result = orderRepository.findAllWithItemsAndCustomer();

        // Assert
        assertThat(result).hasSize(1);

        Order persisted = result.get(0);
        assertThat(persisted.getExternalId()).isEqualTo(ORDER_EXT_ID_1);
        assertThat(persisted.getCustomer().getExternalId()).isEqualTo(CUSTOMER_EXT_ID);
        assertThat(persisted.getItems()).hasSize(1);
        assertThat(persisted.getItems().get(0).getProductSku()).isEqualTo(SKU_MUG);
        assertThat(persisted.getItems().get(0).getQuantity()).isEqualTo(QTY_TWO);
    }

    @Test
    void findAllWithItemsAndCustomer_returnsDistinctOrders_withCorrectItemCounts() {
        // Arrange
        Customer c = savedCustomer(CUSTOMER_EXT_ID_A);

        Order o1 = orderWith(c, ORDER_EXT_ID_1,
                item(SKU_MUG, NAME_MUG, QTY_TWO, PRICE_MUG),
                item(SKU_TSHIRT, NAME_TSHIRT, QTY_ONE, PRICE_TSHIRT));

        Order o2 = orderWith(c, ORDER_EXT_ID_2,
                item(SKU_MUG, NAME_MUG, QTY_ONE, PRICE_MUG));

        orderRepository.save(o1);
        orderRepository.save(o2);

        // Act
        List<Order> result = orderRepository.findAllWithItemsAndCustomer();

        // Assert
        assertThat(result).extracting(Order::getExternalId)
                .containsExactlyInAnyOrder(ORDER_EXT_ID_1, ORDER_EXT_ID_2);

        assertThat(getByExternalId(result, ORDER_EXT_ID_1).getItems()).hasSize(2);
        assertThat(getByExternalId(result, ORDER_EXT_ID_2).getItems()).hasSize(1);
    }

    @Test
    void removingItem_fromOrder_deletesOrphan_whenOrphanRemovalEnabled() {
        // Arrange
        Customer saved = savedCustomer(CUSTOMER_EXT_ID);
        Order o1 = orderWith(saved, ORDER_EXT_ID_1,
                item(SKU_MUG, NAME_MUG, QTY_TWO, PRICE_MUG),
                item(SKU_TSHIRT, NAME_TSHIRT, QTY_ONE, PRICE_TSHIRT));

        o1 = orderRepository.saveAndFlush(o1);

        int initialSize = o1.getItems().size();
        OrderItem toRemove = o1.getItems().get(0);
        o1.removeItem(toRemove);
        orderRepository.saveAndFlush(o1);

        // Act
        List<Order> result = orderRepository.findAllWithItemsAndCustomer();

        // Assert
        assertThat(getByExternalId(result, ORDER_EXT_ID_1).getItems()).hasSize(initialSize - 1);
    }

    private Customer savedCustomer(String externalId) {
        return customerRepository.save(
                new CustomerBuilder().withExternalId(externalId).build()
        );
    }

    private OrderItem item(String sku, String name, int quantity, BigDecimal unitPrice) {
        return new OrderItemBuilder()
                .withSku(sku)
                .withName(name)
                .withQuantity(quantity)
                .withUnitPrice(unitPrice)
                .withCurrency(Currency.EUR)
                .build();
    }

    private Order orderWith(Customer customer, String externalId, OrderItem... items) {
        OrderBuilder ob = new OrderBuilder()
                .withExternalId(externalId)
                .withCustomer(customer);
        for (OrderItem it : items) {
            ob.addItem(it);
        }
        return ob.build();
    }

    private Order getByExternalId(List<Order> orders, String externalId) {
        return orders.stream()
                .filter(o -> externalId.equals(o.getExternalId()))
                .findFirst()
                .orElseThrow();
    }
}
