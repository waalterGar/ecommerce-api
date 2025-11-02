package com.waalterGar.projects.ecommerce.service;

import com.waalterGar.projects.ecommerce.Dto.CartDto;
import com.waalterGar.projects.ecommerce.utils.Currency;

public interface CartService {
    CartDto createCart(Currency currency);

    CartDto getCartByExternalId(String externalId);

    CartDto addItem(String externalId, String sku, int qty);
    CartDto updateQty(String externalId, String sku, int qty);
    CartDto removeItem(String externalId, String sku);

    CartDto clearCart(String externalId);
}
