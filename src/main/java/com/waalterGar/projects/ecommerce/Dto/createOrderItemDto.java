package com.waalterGar.projects.ecommerce.Dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
public class createOrderItemDto {
    private String productSku;
    private Integer quantity;
}
