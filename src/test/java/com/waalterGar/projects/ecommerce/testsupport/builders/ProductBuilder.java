package com.waalterGar.projects.ecommerce.testsupport.builders;

import com.waalterGar.projects.ecommerce.entity.Product;
import com.waalterGar.projects.ecommerce.utils.Currency;

import java.math.BigDecimal;

public class ProductBuilder {
    private String sku = "SKU-"+System.nanoTime();
    private String name = "Test Product";
    private BigDecimal price = new BigDecimal("19.99");
    private Currency currency = Currency.EUR;
    private boolean isActive = true;

    public ProductBuilder withSku(String s) { this.sku = s; return this; }
    public ProductBuilder withName(String n) { this.name = n; return this; }
    public ProductBuilder withPrice(String p) { this.price = new BigDecimal(p); return this; }
    public ProductBuilder withCurrency(Currency c) { this.currency = c; return this; }
    public ProductBuilder inactive() { this.isActive = false; return this; }

    public Product build() {
        Product p = new Product();
        p.setSku(sku); p.setName(name); p.setPrice(price);
        p.setCurrency(currency); p.setIsActive(isActive);
        return p;
    }
}