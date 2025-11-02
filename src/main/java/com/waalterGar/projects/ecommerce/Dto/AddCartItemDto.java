package com.waalterGar.projects.ecommerce.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AddCartItemDto {
    @NotBlank(message = "sku is required")
    private String sku;

    @Min(value=1, message = "qty must be at least 1")
    private int qty;
}
