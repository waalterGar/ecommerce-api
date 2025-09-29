package com.waalterGar.projects.ecommerce.Dto;


import com.waalterGar.projects.ecommerce.utils.Currency;
import lombok.*;

import java.math.BigDecimal;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
public class OrderItemDto {
    private String productSku;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private Currency currency;
    private BigDecimal lineTotal;
}
