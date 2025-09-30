package com.waalterGar.projects.ecommerce.Dto;

import com.waalterGar.projects.ecommerce.utils.Currency;
import com.waalterGar.projects.ecommerce.utils.PriceFormatter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
public class ProductDto {
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Currency currency;
    private Integer stockQuantity;
    private Boolean isActive;

    public ProductDto(String sku, String name, String description, BigDecimal price,
                      Currency currency, Integer stockQuantity, Boolean isActive) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.currency = currency;
        this.stockQuantity = stockQuantity;
        this.isActive = isActive;
    }


}
