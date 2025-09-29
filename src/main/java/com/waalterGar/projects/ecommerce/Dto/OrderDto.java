package com.waalterGar.projects.ecommerce.Dto;


import com.waalterGar.projects.ecommerce.utils.Currency;
import com.waalterGar.projects.ecommerce.utils.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
public class OrderDto {
    private String externalId;
    private String customerExternalId;
    private OrderStatus status;
    private Currency currency;
    private BigDecimal totalAmount;
    private List<OrderItemDto> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
