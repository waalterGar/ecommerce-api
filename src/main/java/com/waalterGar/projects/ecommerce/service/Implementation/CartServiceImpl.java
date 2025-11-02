package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.CartDto;
import com.waalterGar.projects.ecommerce.entity.Cart;
import com.waalterGar.projects.ecommerce.entity.CartItem;
import com.waalterGar.projects.ecommerce.entity.Product;
import com.waalterGar.projects.ecommerce.mapper.CartMapper;
import com.waalterGar.projects.ecommerce.repository.CartRepository;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.service.CartService;
import com.waalterGar.projects.ecommerce.service.exception.InactiveProductException;
import com.waalterGar.projects.ecommerce.utils.Currency;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    @Override
    public CartDto createCart(Currency currency) {
        Cart cart = new Cart();
        cart.setExternalId(UUID.randomUUID().toString());
        cart.setCurrency(currency != null ? currency : Currency.EUR);
        Cart saved = cartRepository.save(cart);
        return CartMapper.toDto(saved);
    }

    @Override
    public CartDto getCartByExternalId(String externalId) {
        Cart cart = cartRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        return CartMapper.toDto(cart);
    }

    @Transactional
    @Override
    public CartDto addItem(String externalId, String sku, int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if(sku == null || sku.isEmpty()){
            throw new IllegalArgumentException("SKU must not be null or empty");
        }

        Cart cart = cartRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found"));

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));

        if (Boolean.FALSE.equals(product.getIsActive())) {
            throw new InactiveProductException("Product is inactive: " + product.getSku());
        }

        if (cart.getCurrency() == null) {
            cart.setCurrency(product.getCurrency());
        } else if (product.getCurrency() != cart.getCurrency()) {
            throw new IllegalArgumentException("Product currency does not match cart currency");
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(ci -> sku.equals(ci.getProductSku()))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + qty);
            item.setUnitPrice(product.getPrice().setScale(2, RoundingMode.HALF_UP)); // snapshot current unit price on update
            item.computeLineTotal();
        } else {
            CartItem item = new CartItem();
            item.setProductSku(product.getSku());
            item.setProductName(product.getName());
            item.setQuantity(qty);
            item.setUnitPrice(product.getPrice().setScale(2, RoundingMode.HALF_UP));
            item.computeLineTotal();

            cart.addItem(item);
        }
        return CartMapper.toDto(cartRepository.save(cart));
    }

    @Override
    public CartDto updateQty(String externalId, String sku, int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if(sku == null || sku.isEmpty()){
            throw new IllegalArgumentException("SKU must not be null or empty");
        }

        Cart cart = cartRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(ci -> sku.equals(ci.getProductSku()))
                .findFirst().orElseThrow(() -> new NoSuchElementException("Item not found in cart: " + sku));

        if (qty == 0) {
            cart.removeItem(item);
        } else {
            Product product = productRepository.findBySku(sku)
                    .orElseThrow(() -> new NoSuchElementException("Product not found"));

            if (Boolean.FALSE.equals(product.getIsActive())) {
                throw new InactiveProductException("Product is inactive: " + product.getSku());
            }
            // Keep latest snapshot price/name
            item.setQuantity(qty);
            item.setUnitPrice(product.getPrice().setScale(2, RoundingMode.HALF_UP));
            item.setProductName(product.getName());
            item.computeLineTotal();
        }

        Cart saved = cartRepository.save(cart);
        return CartMapper.toDto(saved);

    }

    @Override
    public CartDto removeItem(String externalId, String sku) {
        if(sku == null || sku.isEmpty()){
            throw new IllegalArgumentException("SKU must not be null or empty");
        }

        Cart cart = cartRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found"));

        cart.getItems().stream()
                .filter(ci -> sku.equals(ci.getProductSku()))
                .findFirst()
                .ifPresent(cart::removeItem);

        Cart saved = cartRepository.save(cart);
        return CartMapper.toDto(saved);
    }

    @Override
    public CartDto clearCart(String externalId) {
        Cart cart = cartRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found"));
        cart.clearItems();
        Cart saved = cartRepository.save(cart);
        return CartMapper.toDto(saved);
    }
}
