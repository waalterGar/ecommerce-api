package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderItemDto;
import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.entity.OrderItem;
import com.waalterGar.projects.ecommerce.entity.Product;
import com.waalterGar.projects.ecommerce.repository.CustomerRepository;
import com.waalterGar.projects.ecommerce.repository.OrderRepository;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.testsupport.builders.CustomerBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.OrderBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.OrderItemBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.ProductBuilder;
import com.waalterGar.projects.ecommerce.utils.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.waalterGar.projects.ecommerce.utils.Currency.EUR;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    OrderRepository orderRepository;
    @Mock
    CustomerRepository customerRepository;
    @Mock
    ProductRepository productRepository;

    @InjectMocks
    OrderServiceImpl orderService;

    // Default test values
    private static final String ORDER_EXT_ID = "ord-123";
    private static final String CUSTOMER_EXT_ID = "cust-123";
    private static final String PRODUCT_SKU = "MUG-LOGO-001";
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("19.99");
    private static final String PRODUCT_NAME = "Logo Mug";
    private static final String PRODUCT_DESCRIPTION = "A mug with the company logo";
    private static final Currency PRODUCT_CURRENCY = EUR;
    private static final Integer PRODUCT_STOCK = 100;
    private static final Boolean PRODUCT_ACTIVE = true;
    private static final int ITEM_QUANTITY = 2;

    @Test
    @DisplayName("getOrderByExternalId: returns DTO when order exists")
    void getOrderByExternalId_whenFound_returnsDto() {
        // Given
        Customer customer = new CustomerBuilder()
                .withExternalId(CUSTOMER_EXT_ID)
                .build();
        Order order = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withCustomer(customer)
                .addItem(new OrderItemBuilder()
                        .withSku(PRODUCT_SKU)
                        .withName(PRODUCT_NAME)
                        .withQuantity(ITEM_QUANTITY)
                        .withUnitPrice(PRODUCT_PRICE)
                        .withCurrency(PRODUCT_CURRENCY)
                        .build())
                .build();

        BigDecimal expectedLineTotal = PRODUCT_PRICE.multiply(BigDecimal.valueOf(ITEM_QUANTITY));
        BigDecimal expectedOrderTotal = expectedLineTotal; // only one item in this test

                when(orderRepository.findByExternalId(eq(ORDER_EXT_ID)))
                .thenReturn(Optional.of(order));

        // When
        OrderDto dto = orderService.getOrderByExternalId(ORDER_EXT_ID);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getProductSku()).isEqualTo(PRODUCT_SKU);
        assertThat(dto.getItems().get(0).getQuantity()).isEqualTo(ITEM_QUANTITY);
        assertThat(dto.getExternalId()).isEqualTo(ORDER_EXT_ID);

        assertThat(dto.getItems().get(0).getLineTotal()).isEqualByComparingTo(expectedLineTotal);
        assertThat(dto.getTotalAmount()).isEqualByComparingTo(expectedOrderTotal);


        verify(orderRepository).findByExternalId(eq(ORDER_EXT_ID));
        verifyNoMoreInteractions(orderRepository, customerRepository, productRepository);
    }

    @Test
    @DisplayName("getOrderByExternalId: throws with message 'Order not found' when missing")
    void getOrderByExternalId_whenMissing_throwsWithExactMessage() {
        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByExternalId(ORDER_EXT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Order not found");

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verifyNoMoreInteractions(orderRepository, customerRepository, productRepository);
    }

    @Test
    @DisplayName("getAllOrders: returns empty list when repo has no orders")
    void getAllOrders_whenNoOrders_returnsEmptyList() {
        when(orderRepository.findAllWithItemsAndCustomer()).thenReturn(List.of());

        List<OrderDto> result = orderService.getAllOrders();

        assertThat(result).isEmpty();
        verify(orderRepository).findAllWithItemsAndCustomer();
        verifyNoMoreInteractions(orderRepository, customerRepository, productRepository);
    }

    @Test
    @DisplayName("createOrder: happy path persists aggregate and returns DTO")
    void createOrder_happyPath_persists_andReturnsDto() {
        // Given
        Customer customer = new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build();

        Product product = new ProductBuilder()
                .withSku(PRODUCT_SKU)
                .withName(PRODUCT_NAME)
                .withPrice(PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .build();

        createOrderDto dto = new createOrderDto();
        dto.setCustomerExternalId(CUSTOMER_EXT_ID);

        createOrderItemDto item = new createOrderItemDto();
        item.setQuantity(ITEM_QUANTITY);
        item.setProductSku(PRODUCT_SKU);
        dto.setItems(List.of(
                 item));

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID))
                .thenReturn(Optional.of(customer));
        when(productRepository.findBySku(PRODUCT_SKU))
                .thenReturn(Optional.of(product));

        var orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setExternalId(ORDER_EXT_ID);
            return o;
        });

        // When
        var result = orderService.createOrder(dto);

        // Then
        assertThat(result).isNotNull();
        verify(orderRepository).save(orderCaptor.capture());
        var persisted = orderCaptor.getValue();

        assertThat(persisted.getCustomer().getExternalId()).isEqualTo(CUSTOMER_EXT_ID);
        assertThat(persisted.getItems()).hasSize(1);
        assertThat(persisted.getItems().get(0).getProductSku()).isEqualTo(PRODUCT_SKU);
        assertThat(persisted.getItems().get(0).getQuantity()).isEqualTo(ITEM_QUANTITY);

        BigDecimal expectedLineTotal = PRODUCT_PRICE.multiply(BigDecimal.valueOf(ITEM_QUANTITY));
        BigDecimal expectedOrderTotal = expectedLineTotal;
        assertThat(persisted.getItems().get(0).getLineTotal()).isEqualByComparingTo(expectedLineTotal);
        assertThat(persisted.getTotalAmount()).isEqualByComparingTo(expectedOrderTotal);

        assertThat(persisted.getItems().get(0).getUnitPrice()).isEqualByComparingTo(PRODUCT_PRICE);
        assertThat(persisted.getItems().get(0).getCurrency()).isEqualTo(PRODUCT_CURRENCY);
        assertThat(persisted.getItems().get(0).getProductName()).isEqualTo(PRODUCT_NAME);

        // Optional: assert result mirrors persisted externalId
        assertThat(result.getExternalId()).isEqualTo(ORDER_EXT_ID);

        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verify(productRepository).findBySku(PRODUCT_SKU);
        verify(orderRepository).save(any(Order.class));
        verifyNoMoreInteractions(orderRepository, customerRepository, productRepository);
    }

    @Test
    @DisplayName("createOrder: throws when customer not found")
    void createOrder_whenCustomerMissing_throws() {
        createOrderDto dto = new createOrderDto();
        dto.setCustomerExternalId(CUSTOMER_EXT_ID);

        createOrderItemDto item = new createOrderItemDto();
        item.setProductSku(PRODUCT_SKU);
        item.setQuantity(ITEM_QUANTITY);

        dto.setItems(List.of(item));

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(NoSuchElementException.class);

        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verifyNoMoreInteractions(orderRepository, customerRepository, productRepository);
    }

    @Test
    @DisplayName("createOrder: throws when any product SKU is not found")
    void createOrder_whenProductMissing_throws() {
        createOrderDto dto = new createOrderDto();
        dto.setCustomerExternalId(CUSTOMER_EXT_ID);
        createOrderItemDto item  = new createOrderItemDto();
        item.setProductSku(PRODUCT_SKU);
        item.setQuantity(ITEM_QUANTITY);
        dto.setItems(List.of(item));

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID))
                .thenReturn(Optional.of(new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build()));
        when(productRepository.findBySku(PRODUCT_SKU)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(NoSuchElementException.class);

        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verify(productRepository).findBySku(PRODUCT_SKU);
        verifyNoMoreInteractions(orderRepository, customerRepository, productRepository);
    }

}