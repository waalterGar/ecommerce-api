package com.waalterGar.projects.ecommerce.service;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderItemDto;
import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.entity.Product;
import com.waalterGar.projects.ecommerce.repository.CustomerRepository;
import com.waalterGar.projects.ecommerce.repository.OrderRepository;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.service.exception.InsufficientStockException;
import com.waalterGar.projects.ecommerce.testsupport.builders.CustomerBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.ProductBuilder;
import com.waalterGar.projects.ecommerce.utils.Currency;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.junit.jupiter.Container;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.waalterGar.projects.ecommerce.utils.Currency.EUR;
import static com.waalterGar.projects.ecommerce.utils.Currency.USD;

@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class OrderServiceIT {

    // Reuse the same container style as your OrderRepositoryIT
    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.37");

    @Autowired
    OrderService orderService;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    OrderRepository orderRepository;

    private static final String ORDER_EXT_ID = "ord-123";
    private static final String CUSTOMER_EXT_ID = "cust-123";
    private static final String FIRST_PRODUCT_SKU = "MUG-LOGO-001";
    private static final String SECOND_PRODUCT_SKU = "TSHIRT-LOGO-001";
    private static final BigDecimal FIRST_PRODUCT_PRICE = new BigDecimal("19.99");
    private static final BigDecimal SECOND_PRODUCT_PRICE = new BigDecimal("29.99");
    private static final String FIRST_PRODUCT_NAME = "Logo Mug";
    private static final String SECOND_PRODUCT_NAME = "Logo T-Shirt";
    private static final Currency PRODUCT_CURRENCY = EUR;
    private static final Currency OTHER_CURRENCY = USD;
    private static final int FIRST_ITEM_QUANTITY = 3;

    @AfterEach
    void cleanup() { orderRepository.deleteAll(); productRepository.deleteAll(); customerRepository.deleteAll();}

    @Test
    void createOrder_happyPath_decrementsStock_andPersistsOrder() {
        // Given
        Customer c = new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build();
        customerRepository.save(c);

        Product p = new ProductBuilder().withSku(FIRST_PRODUCT_SKU)
                                        .withName(FIRST_PRODUCT_NAME)
                                        .withPrice(FIRST_PRODUCT_PRICE.toPlainString())
                                        .withCurrency(PRODUCT_CURRENCY)
                                        .withStockQuantity(10).build();
        productRepository.save(p);

        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, FIRST_PRODUCT_SKU, FIRST_ITEM_QUANTITY);
        BigDecimal expectedLineTotal = FIRST_PRODUCT_PRICE.multiply(BigDecimal.valueOf(FIRST_ITEM_QUANTITY));

        OrderDto result = orderService.createOrder(dto);

        List<Order> orders = orderRepository.findAllWithItemsAndCustomer();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getTotalAmount()).isEqualByComparingTo(expectedLineTotal);
        assertThat(orders.get(0).getItems()).hasSize(1);

        Product reloaded = productRepository.findBySku(FIRST_PRODUCT_SKU).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(7);

        assertThat(result.getExternalId()).isNotBlank();
    }

    @Test
    void createOrder_failure_rollsBack_noOrder_noStockChange() {
        Customer c = new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build();
        customerRepository.save(c);

        Product p = new ProductBuilder()
                .withSku(FIRST_PRODUCT_SKU)
                .withName(FIRST_PRODUCT_NAME)
                .withPrice(FIRST_PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(2)
                .build();
        productRepository.save(p);

        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, FIRST_PRODUCT_SKU, 5);

        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(InsufficientStockException.class);

        // No order persisted; stock unchanged
        assertThat(orderRepository.count()).isZero();
        Product reloaded = productRepository.findBySku(FIRST_PRODUCT_SKU).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(2);
    }

    @Test
    void createOrder_boundary_equalsStock_succeeds_toZero() {
        Customer c = new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build();
        customerRepository.save(c);

        Product p = new ProductBuilder()
                .withSku(SECOND_PRODUCT_SKU)
                .withName(SECOND_PRODUCT_NAME)
                .withPrice(SECOND_PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(2)
                .build();
        productRepository.save(p);

        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, SECOND_PRODUCT_SKU, 2);
        BigDecimal expectedLineTotal = SECOND_PRODUCT_PRICE.multiply(BigDecimal.valueOf(2));

        OrderDto result = orderService.createOrder(dto);

        // order persisted
        List<Order> orders = orderRepository.findAllWithItemsAndCustomer();
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getItems()).hasSize(1);
        assertThat(orders.get(0).getTotalAmount()).isEqualByComparingTo(expectedLineTotal);

        // assert: stock now zero
        Product reloaded = productRepository.findBySku(SECOND_PRODUCT_SKU).orElseThrow();
        assertThat(reloaded.getStockQuantity()).isEqualTo(0);

        // optional: returned DTO has externalId
        assertThat(result.getExternalId()).isNotBlank();
    }

    private createOrderDto singleItemOrderDto(String customerExternalId, String sku, int quantity) {
        createOrderDto dto = new createOrderDto();
        dto.setCustomerExternalId(customerExternalId);

        createOrderItemDto item = new createOrderItemDto();
        item.setProductSku(sku);
        item.setQuantity(quantity);

        dto.setItems(List.of(item));

        return dto;
    }
}
