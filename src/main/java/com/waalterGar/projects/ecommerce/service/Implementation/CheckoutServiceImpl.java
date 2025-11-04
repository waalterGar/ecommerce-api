package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderItemDto;
import com.waalterGar.projects.ecommerce.entity.Cart;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.repository.CartRepository;
import com.waalterGar.projects.ecommerce.repository.OrderRepository;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.service.CheckoutService;
import com.waalterGar.projects.ecommerce.service.OrderService;
import com.waalterGar.projects.ecommerce.utils.CartStatus;
import com.waalterGar.projects.ecommerce.utils.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Service
public class CheckoutServiceImpl implements CheckoutService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Override
    @Transactional
    public OrderDto checkout(String cartExternalId, String customerExternalId) {
        if (cartExternalId == null || cartExternalId.isBlank()) {
            throw new IllegalArgumentException("cartExternalId is required");
        }

        if (customerExternalId == null || customerExternalId.isBlank()) {
            throw new IllegalArgumentException("customerExternalId is required");
        }

        Cart cart = cartRepository.findByExternalId(cartExternalId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found"));

        if (cart.getStatus() != null && cart.getStatus() != CartStatus.NEW) {
            throw new IllegalStateException("Cart is not editable or already checked out");
        }

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");}

        Currency cartCurrency = cart.getCurrency();
        if (cartCurrency == null) {
            throw new IllegalArgumentException("Cart currency is not set");
        }

        createOrderDto dto = new createOrderDto();
        dto.setCustomerExternalId(customerExternalId);
        dto.setItems(
                cart.getItems().stream().map(ci -> {
                    createOrderItemDto oi = new createOrderItemDto();
                    oi.setProductSku(ci.getProductSku());
                    oi.setQuantity(ci.getQuantity());
                    return oi;
                }).toList()
        );
        OrderDto order = orderService.createOrder(dto);

        // 6) Mark cart as checked out
        cart.setStatus(CartStatus.CHECKED_OUT);
        cart.setCheckedOutAt(LocalDateTime.now());
        cartRepository.save(cart);

        return order;
    }
}
