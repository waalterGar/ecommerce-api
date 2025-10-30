package com.waalterGar.projects.ecommerce.mapper;

import com.waalterGar.projects.ecommerce.Dto.CartDto;
import com.waalterGar.projects.ecommerce.Dto.CartItemDto;
import com.waalterGar.projects.ecommerce.entity.Cart;
import com.waalterGar.projects.ecommerce.entity.CartItem;

import java.math.BigDecimal;
import java.util.stream.Collectors;

public class CartMapper {

    public static CartDto toDto(Cart cart) {
        if (cart == null) {
            return null;
        }
        CartDto dto = new CartDto();
        dto.setExternalId(cart.getExternalId());
        dto.setCurrency(cart.getCurrency());
        dto.setStatus(cart.getStatus());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());
        dto.setCheckedOutAt(cart.getCheckedOutAt());

        dto.setItems(cart.getItems().stream()
                .map(CartMapper::toItemDto)
                .collect(Collectors.toList()));
        return dto;
    }

    public static Cart toEntity(CartDto dto) {
        if (dto == null) {
            return null;
        }
        Cart cart = new Cart();
        cart.setExternalId(dto.getExternalId());
        cart.setCurrency(dto.getCurrency());
        cart.setStatus(dto.getStatus());
        cart.setCreatedAt(dto.getCreatedAt());
        cart.setUpdatedAt(dto.getUpdatedAt());
        cart.setCheckedOutAt(dto.getCheckedOutAt());

        cart.setItems(dto.getItems().stream()
                .map(CartMapper::toItemEntity)
                .collect(Collectors.toList()));
        return cart;
    }

    private static CartItemDto toItemDto(CartItem item) {
        if (item == null) return null;
        CartItemDto dto = new CartItemDto();
        dto.setProductSku(item.getProductSku());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setLineTotal(item.getLineTotal());
        return dto;
    }

    private static CartItem toItemEntity(CartItemDto dto) {
        if (dto == null) return null;
        CartItem item = new CartItem();
        item.setProductSku(dto.getProductSku());
        item.setProductName(dto.getProductName());
        item.setQuantity(dto.getQuantity());
        item.setUnitPrice(dto.getUnitPrice());
        item.setLineTotal(dto.getLineTotal());
        return item;
    }

    // Compute totals dynamically
    private static void computeTotals(Cart cart, CartDto dto) {
        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal total = subtotal.add(tax);

        dto.setSubtotal(subtotal);
        dto.setTax(tax);
        dto.setTotal(total);
    }
}
