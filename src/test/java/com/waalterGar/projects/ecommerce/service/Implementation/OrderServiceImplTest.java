package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.PayOrderRequestDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderItemDto;
import com.waalterGar.projects.ecommerce.entity.Customer;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.entity.Payment;
import com.waalterGar.projects.ecommerce.entity.Product;
import com.waalterGar.projects.ecommerce.repository.CustomerRepository;
import com.waalterGar.projects.ecommerce.repository.OrderRepository;
import com.waalterGar.projects.ecommerce.repository.PaymentRepository;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.service.exception.InactiveProductException;
import com.waalterGar.projects.ecommerce.service.exception.InsufficientStockException;
import com.waalterGar.projects.ecommerce.testsupport.builders.CustomerBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.OrderBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.OrderItemBuilder;
import com.waalterGar.projects.ecommerce.testsupport.builders.ProductBuilder;
import com.waalterGar.projects.ecommerce.utils.Currency;
import com.waalterGar.projects.ecommerce.utils.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.waalterGar.projects.ecommerce.utils.Currency.EUR;
import static com.waalterGar.projects.ecommerce.utils.Currency.USD;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock CustomerRepository customerRepository;
    @Mock ProductRepository productRepository;
    @Mock PaymentRepository paymentRepository;

    @InjectMocks OrderServiceImpl orderService;

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
    private static final int FIRST_ITEM_QUANTITY = 2;
    private static final int SECOND_ITEM_QUANTITY = 1;
    private static final String TRANSACTION_REFERENCE = "tx-123";
    private static final String PAYMENT_PROVIDER = "stripe";

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
                        .withSku(FIRST_PRODUCT_SKU)
                        .withName(FIRST_PRODUCT_NAME)
                        .withQuantity(FIRST_ITEM_QUANTITY)
                        .withUnitPrice(FIRST_PRODUCT_PRICE)
                        .withCurrency(PRODUCT_CURRENCY)
                        .build())
                .build();

        BigDecimal expectedLineTotal = FIRST_PRODUCT_PRICE.multiply(BigDecimal.valueOf(FIRST_ITEM_QUANTITY));
        BigDecimal expectedOrderTotal = expectedLineTotal; // single item

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(order));

        // When
        OrderDto dto = orderService.getOrderByExternalId(ORDER_EXT_ID);

        // Then
        assertThat(dto).isNotNull();
        assertThat(dto.getExternalId()).isEqualTo(ORDER_EXT_ID);
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getProductSku()).isEqualTo(FIRST_PRODUCT_SKU);
        assertThat(dto.getItems().get(0).getQuantity()).isEqualTo(FIRST_ITEM_QUANTITY);
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
                .withSku(FIRST_PRODUCT_SKU)
                .withName(FIRST_PRODUCT_NAME)
                .withPrice(FIRST_PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(2)
                .build();

        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, FIRST_PRODUCT_SKU, FIRST_ITEM_QUANTITY);

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID)).thenReturn(Optional.of(customer));
        when(productRepository.findBySku(FIRST_PRODUCT_SKU)).thenReturn(Optional.of(product));

        // Simulate persistence assigning the externalId
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setExternalId(ORDER_EXT_ID);
            return o;
        });

        // When
        OrderDto result = orderService.createOrder(dto);

        // Then (assert only what the client sees)
        BigDecimal expectedLineTotal = FIRST_PRODUCT_PRICE.multiply(BigDecimal.valueOf(FIRST_ITEM_QUANTITY));
        BigDecimal expectedOrderTotal = expectedLineTotal;

        assertThat(result).isNotNull();
        assertThat(result.getExternalId()).isEqualTo(ORDER_EXT_ID);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductSku()).isEqualTo(FIRST_PRODUCT_SKU);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(FIRST_ITEM_QUANTITY);
        assertThat(result.getItems().get(0).getLineTotal()).isEqualByComparingTo(expectedLineTotal);
        assertThat(result.getTotalAmount()).isEqualByComparingTo(expectedOrderTotal);

        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verify(productRepository).findBySku(FIRST_PRODUCT_SKU);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: throws when customer not found")
    void createOrder_whenCustomerMissing_throws() {
        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, FIRST_PRODUCT_SKU, FIRST_ITEM_QUANTITY);

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(NoSuchElementException.class);

        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: throws when any product SKU is not found")
    void createOrder_whenProductMissing_throws() {
        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, FIRST_PRODUCT_SKU, FIRST_ITEM_QUANTITY);

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID))
                .thenReturn(Optional.of(new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build()));
        when(productRepository.findBySku(FIRST_PRODUCT_SKU)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(NoSuchElementException.class);

        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verify(productRepository).findBySku(FIRST_PRODUCT_SKU);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_happyPath_setsPricesFromProduct_computesTotals_andSetsCurrency() {
        // Given
        createOrderItemDto firstItem = new createOrderItemDto();
        firstItem.setProductSku(FIRST_PRODUCT_SKU);
        firstItem.setQuantity(FIRST_ITEM_QUANTITY);

        createOrderItemDto secondItem = new createOrderItemDto();
        secondItem.setProductSku(SECOND_PRODUCT_SKU);
        secondItem.setQuantity(SECOND_ITEM_QUANTITY);

        createOrderDto order = new createOrderDto();
        order.setCustomerExternalId(CUSTOMER_EXT_ID);
        order.setItems(List.of(firstItem, secondItem));

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID)).thenReturn(Optional.of(new Customer()));

        Product firstProduct = new ProductBuilder()
                .withSku(FIRST_PRODUCT_SKU)
                .withName(FIRST_PRODUCT_NAME)
                .withPrice(FIRST_PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(10)
                .build();

        Product secondProduct = new ProductBuilder()
                .withSku(SECOND_PRODUCT_SKU)
                .withName(SECOND_PRODUCT_NAME)
                .withPrice(SECOND_PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(5)
                .build();

        when(productRepository.findBySku(FIRST_PRODUCT_SKU)).thenReturn(Optional.of(firstProduct));
        when(productRepository.findBySku(SECOND_PRODUCT_SKU)).thenReturn(Optional.of(secondProduct));

        when (orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<Order> orderCap = ArgumentCaptor.forClass(Order.class);

        // When
        orderService.createOrder(order);

        // Then
        verify(orderRepository).save(orderCap.capture());
        Order savedOrder = orderCap.getValue();

        BigDecimal expectedFirstLineTotal = FIRST_PRODUCT_PRICE.multiply(BigDecimal.valueOf(FIRST_ITEM_QUANTITY));
        BigDecimal expectedSecondLineTotal = SECOND_PRODUCT_PRICE.multiply(BigDecimal.valueOf(SECOND_ITEM_QUANTITY));
        BigDecimal expectedOrderTotal = expectedFirstLineTotal.add(expectedSecondLineTotal);

        assertThat(savedOrder.getCurrency()).isEqualTo(PRODUCT_CURRENCY);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(savedOrder.getItems()).hasSize(2);

        assertThat(savedOrder.getItems().get(0).getUnitPrice()).isEqualByComparingTo(FIRST_PRODUCT_PRICE);
        assertThat(savedOrder.getItems().get(0).getLineTotal()).isEqualByComparingTo(expectedFirstLineTotal);
        assertThat(savedOrder.getItems().get(1).getUnitPrice()).isEqualByComparingTo(SECOND_PRODUCT_PRICE);
        assertThat(savedOrder.getItems().get(1).getLineTotal()).isEqualByComparingTo(expectedSecondLineTotal);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(expectedOrderTotal);

        assertThat(savedOrder.getExternalId()).isNotNull();
    }

    @Test
    void createOrder_insufficientStock_throws_andDoesNotSave() {
        // Given
        int availableStock = 2;
        int excessiveQuantity = 5;

        createOrderItemDto firstItem = new createOrderItemDto();
        firstItem.setProductSku(FIRST_PRODUCT_SKU);
        firstItem.setQuantity(excessiveQuantity); // more than available

        createOrderDto order = new createOrderDto();
        order.setCustomerExternalId(CUSTOMER_EXT_ID);
        order.setItems(List.of(firstItem));

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID)).thenReturn(Optional.of(new Customer()));

        Product firstProduct = new ProductBuilder()
                .withSku(FIRST_PRODUCT_SKU)
                .withName(FIRST_PRODUCT_NAME)
                .withPrice(FIRST_PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(availableStock) // less than requested
                .build();

        when(productRepository.findBySku(FIRST_PRODUCT_SKU)).thenReturn(Optional.of(firstProduct));

        // When / Then
        assertThatThrownBy(() -> orderService.createOrder(order))
                .isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }


    @Test
    void createOrder_mixedCurrencies_rejected_noSave(){
        // Given
        createOrderItemDto firstItem = new createOrderItemDto();
        firstItem.setProductSku(FIRST_PRODUCT_SKU);
        firstItem.setQuantity(FIRST_ITEM_QUANTITY);

        createOrderItemDto secondItem = new createOrderItemDto();
        secondItem.setProductSku(SECOND_PRODUCT_SKU);
        secondItem.setQuantity(SECOND_ITEM_QUANTITY);

        createOrderDto order = new createOrderDto();
        order.setCustomerExternalId(CUSTOMER_EXT_ID);
        order.setItems(List.of(firstItem, secondItem));

        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID)).thenReturn(Optional.of(new Customer()));

        Product firstProduct = new ProductBuilder()
                .withSku(FIRST_PRODUCT_SKU)
                .withName(FIRST_PRODUCT_NAME)
                .withPrice(FIRST_PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(10)
                .build();

        Product secondProduct = new ProductBuilder()
                .withSku(SECOND_PRODUCT_SKU)
                .withName(SECOND_PRODUCT_NAME)
                .withPrice(SECOND_PRODUCT_PRICE.toPlainString())
                .withCurrency(OTHER_CURRENCY)
                .withStockQuantity(5)
                .build();

        when(productRepository.findBySku(FIRST_PRODUCT_SKU)).thenReturn(Optional.of(firstProduct));
        when(productRepository.findBySku(SECOND_PRODUCT_SKU)).thenReturn(Optional.of(secondProduct));

        // When / Then
        assertThatThrownBy(() -> orderService.createOrder(order))
                .isInstanceOf(IllegalArgumentException.class);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_missingCustomer_rejected_noSave() {
        createOrderItemDto firstItem = new createOrderItemDto();
        firstItem.setProductSku(FIRST_PRODUCT_SKU);
        firstItem.setQuantity(FIRST_ITEM_QUANTITY);

        createOrderDto order = new createOrderDto();
        order.setItems(List.of(firstItem));

        when(customerRepository.findByExternalId(any())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> orderService.createOrder(order))
                .isInstanceOf(NoSuchElementException.class);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_inactiveProduct_throws422_andDoesNotSave() {
        // Arrange: order with a single item for FIRST_PRODUCT_SKU
        createOrderDto dto = singleItemOrderDto(CUSTOMER_EXT_ID, FIRST_PRODUCT_SKU, 1);

        // existing customer
        Customer customer = new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build();
        when(customerRepository.findByExternalId(CUSTOMER_EXT_ID)).thenReturn(Optional.of(customer));

        Product inactive = new ProductBuilder()
                .withSku(FIRST_PRODUCT_SKU)
                .withName(FIRST_PRODUCT_NAME)
                .withPrice(FIRST_PRODUCT_PRICE.toPlainString())
                .withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(10)
                .build();
        inactive.setIsActive(false);  // INACTIVE

        when(productRepository.findBySku(FIRST_PRODUCT_SKU)).thenReturn(Optional.of(inactive));

        // Act + Assert
        assertThatThrownBy(() -> orderService.createOrder(dto))
                .isInstanceOf(InactiveProductException.class)
                .hasMessageContaining("Product is inactive: " + FIRST_PRODUCT_SKU);

        // And: no persistence attempted
        verify(customerRepository).findByExternalId(CUSTOMER_EXT_ID);
        verify(productRepository).findBySku(FIRST_PRODUCT_SKU);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("pay: CREATED -> PAID (creates payment, sets paidAt)")
    void pay_happyPath_marksPaid_andCreatesPayment() {
        // Order in CREATED with totals & currency
        Order o = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withStatus(OrderStatus.CREATED)
                .withCurrency(PRODUCT_CURRENCY)
                .build();
       o.setTotalAmount(FIRST_PRODUCT_PRICE);

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(o));
        when(paymentRepository.findByOrder_ExternalIdAndTransactionReference(any(), any()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PayOrderRequestDto body = new PayOrderRequestDto();
        body.setAmount(FIRST_PRODUCT_PRICE);
        body.setCurrency(PRODUCT_CURRENCY);
        body.setProvider(PAYMENT_PROVIDER);
        body.setTransactionReference(TRANSACTION_REFERENCE);

        OrderDto out = orderService.pay(ORDER_EXT_ID, body);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(o.getPaidAt()).isNotNull();
        assertThat(out.getExternalId()).isEqualTo(ORDER_EXT_ID);
        assertThat(out.getTotalAmount()).isEqualByComparingTo(new BigDecimal("19.99"));

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verify(paymentRepository).findByOrder_ExternalIdAndTransactionReference(ORDER_EXT_ID, TRANSACTION_REFERENCE);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("pay: idempotent success when same transactionReference already recorded for this order")
    void pay_idempotent_sameTxRef_returnsSuccess_noDuplicatePayment() {
        Order o = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withStatus(OrderStatus.CREATED)
                .withCurrency(PRODUCT_CURRENCY)
                .build();
        o.setTotalAmount(FIRST_PRODUCT_PRICE);

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(o));

        Payment existing = new Payment();
        existing.setPaidAt(java.time.LocalDateTime.now());
        when(paymentRepository.findByOrder_ExternalIdAndTransactionReference(ORDER_EXT_ID, TRANSACTION_REFERENCE))
                .thenReturn(Optional.of(existing));

        PayOrderRequestDto body = new PayOrderRequestDto();
        body.setTransactionReference(TRANSACTION_REFERENCE);

        OrderDto out = orderService.pay(ORDER_EXT_ID, body);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(out.getExternalId()).isEqualTo(ORDER_EXT_ID);

        verify(paymentRepository).findByOrder_ExternalIdAndTransactionReference(ORDER_EXT_ID, TRANSACTION_REFERENCE);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("pay: invalid transition from CANCELED -> 400")
    void pay_fromCanceled_throwsInvalidRequest() {
        Order o = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withStatus(OrderStatus.CANCELED)
                .withCurrency(PRODUCT_CURRENCY)
                .build();
        o.setTotalAmount(FIRST_PRODUCT_PRICE);

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> orderService.pay(ORDER_EXT_ID, new PayOrderRequestDto()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("payable");

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("pay: amount mismatch -> 400")
    void pay_amountMismatch_throws() {
        Order o = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withStatus(OrderStatus.CREATED)
                .withCurrency(PRODUCT_CURRENCY)
                .build();
        o.setTotalAmount(FIRST_PRODUCT_PRICE);

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(o));

        PayOrderRequestDto body = new PayOrderRequestDto();
        body.setAmount(new BigDecimal("20.00"));

        assertThatThrownBy(() -> orderService.pay(ORDER_EXT_ID, body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount mismatch");

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("pay: currency mismatch -> 400")
    void pay_currencyMismatch_throws() {
        Order o = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withStatus(OrderStatus.CREATED)
                .withCurrency(OTHER_CURRENCY) // USD
                .build();
        o.setTotalAmount(FIRST_PRODUCT_PRICE);

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(o));

        PayOrderRequestDto body = new PayOrderRequestDto();
        body.setCurrency(PRODUCT_CURRENCY); // EUR

        assertThatThrownBy(() -> orderService.pay(ORDER_EXT_ID, body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency mismatch");

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("pay: order not found -> 404")
    void pay_orderNotFound_throws() {
        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.pay(ORDER_EXT_ID, null))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Order not found");

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verifyNoMoreInteractions(paymentRepository);
    }

    @Test
    @DisplayName("cancel: CREATED → CANCELED and restocks products")
    void cancel_happyPath_restocks_andCancels() {
        // Order with two items
        Customer customer = new CustomerBuilder().withExternalId(CUSTOMER_EXT_ID).build();
        Order order = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withCustomer(customer)
                .withStatus(OrderStatus.CREATED)
                .addItem(new OrderItemBuilder()
                        .withSku(FIRST_PRODUCT_SKU)
                        .withName(FIRST_PRODUCT_NAME)
                        .withQuantity(2)
                        .withUnitPrice(FIRST_PRODUCT_PRICE)
                        .withCurrency(PRODUCT_CURRENCY)
                        .build())
                .addItem(new OrderItemBuilder()
                        .withSku(SECOND_PRODUCT_SKU)
                        .withName(SECOND_PRODUCT_NAME)
                        .withQuantity(1)
                        .withUnitPrice(SECOND_PRODUCT_PRICE)
                        .withCurrency(PRODUCT_CURRENCY)
                        .build())
                .build();

        Product p1 = new ProductBuilder().withSku(FIRST_PRODUCT_SKU).withName(FIRST_PRODUCT_NAME)
                .withPrice(FIRST_PRODUCT_PRICE.toPlainString()).withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(5).build(); // will become 7
        Product p2 = new ProductBuilder().withSku(SECOND_PRODUCT_SKU).withName(SECOND_PRODUCT_NAME)
                .withPrice(SECOND_PRODUCT_PRICE.toPlainString()).withCurrency(PRODUCT_CURRENCY)
                .withStockQuantity(10).build(); // will become 11

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(order));
        when(productRepository.findBySku(FIRST_PRODUCT_SKU)).thenReturn(Optional.of(p1));
        when(productRepository.findBySku(SECOND_PRODUCT_SKU)).thenReturn(Optional.of(p2));

        OrderDto out = orderService.cancelOrder(ORDER_EXT_ID);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
        assertThat(order.getCanceledAt()).isNotNull();
        assertThat(p1.getStockQuantity()).isEqualTo(7);
        assertThat(p2.getStockQuantity()).isEqualTo(11);
        assertThat(out.getExternalId()).isEqualTo(ORDER_EXT_ID);

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verify(productRepository).findBySku(FIRST_PRODUCT_SKU);
        verify(productRepository).findBySku(SECOND_PRODUCT_SKU);
    }

    @Test
    @DisplayName("cancel: idempotent when already CANCELED")
    void cancel_alreadyCanceled_idempotent() {
        var order = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withStatus(OrderStatus.CANCELED)
                .build();

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(order));

        OrderDto out = orderService.cancelOrder(ORDER_EXT_ID);

        assertThat(out.getExternalId()).isEqualTo(ORDER_EXT_ID);
        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("cancel: PAID → 400 invalid transition")
    void cancel_fromPaid_throws() {
        var order = new OrderBuilder()
                .withExternalId(ORDER_EXT_ID)
                .withStatus(OrderStatus.PAID)
                .build();

        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_EXT_ID))
                .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Only orders in CREATED status can be canceled");

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verifyNoInteractions(productRepository);
    }

    @Test
    @DisplayName("cancel: order not found → 404")
    void cancel_orderNotFound() {
        when(orderRepository.findByExternalId(ORDER_EXT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_EXT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Order not found");

        verify(orderRepository).findByExternalId(ORDER_EXT_ID);
        verifyNoInteractions(productRepository);
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
