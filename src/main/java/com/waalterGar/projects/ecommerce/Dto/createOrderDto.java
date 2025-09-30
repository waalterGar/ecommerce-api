package com.waalterGar.projects.ecommerce.Dto;

import com.waalterGar.projects.ecommerce.utils.Currency;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class createOrderDto {
    private String customerExternalId;
    private Currency currency;
    private List<createOrderItemDto> items;
}
