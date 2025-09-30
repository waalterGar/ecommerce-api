package com.waalterGar.projects.ecommerce.testsupport.builders;

import com.waalterGar.projects.ecommerce.entity.OrderItem;
import com.waalterGar.projects.ecommerce.utils.Currency;

import java.math.BigDecimal;

public class OrderItemBuilder {
    private String sku = "SKU-TEST";
    private String name = "Test Item";
    private BigDecimal unitPrice = new BigDecimal("10.00");
    private Currency currency = Currency.EUR;
    private int quantity = 1;

    public OrderItemBuilder withSku(String s) { this.sku = s; return this; }
    public OrderItemBuilder withName(String n) { this.name = n; return this; }
    public OrderItemBuilder withUnitPrice(BigDecimal p) { this.unitPrice = p; return this; }
    public OrderItemBuilder withCurrency(Currency c) { this.currency = c; return this; }
    public OrderItemBuilder withQuantity(int q) { this.quantity = q; return this; }

    public OrderItem build() {
        OrderItem i = new OrderItem();
        i.setProductSku(sku);
        i.setProductName(name);
        i.setUnitPrice(unitPrice);
        i.setCurrency(currency);
        i.setQuantity(quantity);
        i.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return i;
    }
}