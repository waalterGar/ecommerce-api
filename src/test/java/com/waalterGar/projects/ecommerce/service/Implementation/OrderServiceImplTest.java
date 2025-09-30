package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderItemDto;
import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.entity.Order;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.waalterGar.projects.ecommerce.utils.Currency.EUR;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock CustomerRepository customerRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks OrderServiceImpl orderService;

    private static final String ORDER_EXT_ID = "ord-123";
    private static final String CUSTOMER_EXT_ID = "cust-123";
    private static final String PRODUCT_SKU = "MUG-LOGO-001";
    private static final BigDecimal PRODUCT_PRICE = new BigDecimal("19.99");
    private static final String PRODUCT_NAME = "Logo Mug";
    private static final Currency PRODUCT_CURRENCY = EUR;
    private static final int ITEM_QUANTITY = 2;

    @Test
    @DisplayName("getOrderByExternalId: returns DTO when order exists")
    void getOrderByExternalId_whenFound_returnsDto() {
        // Given
        Customer customer = new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build();
        Order order = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withCustomer(customer)
                .addItem(
                        new OrderItemBuilder()
                        .withSku(PRODUCT_SKU)
                        .withName(PRODUCT_NAME)
                        .withQuantity(ITEM_QUANTITY)
                        .withUnitPrice(PRODUCT_PRICE)
                        .withCurrency(PRODUCT_CURRENCY)
                        .build())
                .build();

        BigDecimal expectedLineTotal = PRODUCT_PRICE.multiply(BigDecimal.valueOf(ITEM_QUANTITY));
        BigDecimal expectedOrderTotal = expectedLineTotal; // single item

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(order));

        // When
        OrderDto dto = orderService.getOrderByExternalId(ORDER_EXT_ID);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getExternalId()).isEqualTo(ORDER_EXT_ID);
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getProductSku()).isEqualTo(PRODUCT_SKU);
        assertThat(dto.getItems().get(0).getQuantity()).isEqualTo(ITEM_QUANTITY);
        assertThat(dto.getItems().get(0).getLineTotal()).isEqualByComparingTo(expectedLineTotal);
        assertThat(dto.getTotalAmount()).isEqualByComparingTo(expectedOrderTotal);

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
    }

    @Test
    @DisplayName("getOrderByExternalId: throws with message 'Order not found' when missing")
    void getOrderByExternalId_whenMissing_throwsWithExactMessage() {
        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderByExternalId(ORDER_EXT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Order not found");

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
    }

    @Test
    @DisplayName("getAllOrders: returns empty list when repo has no orders")
    void getAllOrders_whenNoOrders_returnsEmptyList() {
        when(orderRepository.findAllWithItemsAndCustomer()).thenReturn(List.of());

        List<OrderDto> result = orderService.getAllOrders();

        assertThat(result).isEmpty();
        verify(orderRepository).findAllWithItemsAndCustomer();
    }

    @Test
    @DisplayName("createOrder: happy path returns DTO with items and totals")
    void createOrder_happyPath_returnsDto() {
        // Given
        Customer customer = new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build();
        Product product = new ProductBuilder()
                .withSku(PRODUCT_SKU)
                .withName(PRODUCT_NAME)
                .withPrice(PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .build();

        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, PRODUCT_SKU, ITEM_QUANTITY);

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findBySku(PRODUCT_SKU)).thenReturn(Optional.of(product));

        // Simulate persistence assigning the externalId
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setExternalId(ORDER_EXT_ID);
            return o;
        });

        // When
        OrderDto result = orderService.createOrder(dto);

        // Then (assert only what the client sees)
        BigDecimal expectedLineTotal = PRODUCT_PRICE.multiply(BigDecimal.valueOf(ITEM_QUANTITY));
        BigDecimal expectedOrderTotal = expectedLineTotal;

        assertThat(result).isNotNull();
        assertThat(result.getExternalId()).isEqualTo(ORDER_EXT_ID);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductSku()).isEqualTo(PRODUCT_SKU);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(ITEM_QUANTITY);
        assertThat(result.getItems().get(0).getLineTotal()).isEqualByComparingTo(expectedLineTotal);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(expectedOrderTotal);

        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verify(productRepository).findBySku(PRODUCT_SKU);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: throws when customer not found")
    void createOrder_whenCustomerMissing_throws() {
        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, PRODUCT_SKU, ITEM_QUANTITY);

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(NoSuchElementException.class);

        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: throws when any product SKU is not found")
    void createOrder_whenProductMissing_throws() {
        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, PRODUCT_SKU, ITEM_QUANTITY);

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID))
                .thenReturn(Optional.of(new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build()));
        when(productRepository.findBySku(PRODUCT_SKU)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(NoSuchElementException.class);

        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verify(productRepository).findBySku(PRODUCT_SKU);
        verify(orderRepository, never()).save(any(Order.class));
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
