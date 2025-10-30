package com.waalterGar.projects.ecommerce.Dto;

import com.waalterGar.projects.ecommerce.utils.CartStatus;
import com.waalterGar.projects.ecommerce.utils.Currency;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
public class CartDto {
    private String externalId;
    private Currency currency;
    private CartStatus status;
    private List<CartItemDto> items;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime checkedOutAt;

    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
}
