package com.waalterGar.projects.ecommerce.Dto;

import com.waalterGar.projects.ecommerce.utils.Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayOrderRequestDto {
    @Digits(integer = 12, fraction = 2)
    @Positive
    private BigDecimal amount; // optional: if present, must equal order.totalAmount

    private Currency currency; // optional: if present, must equal order.currency

    @Size(max = 100)
    private String provider; // optional

    @Size(max = 200)
    private String transactionReference; // optional; enables per-order idempotency
}

