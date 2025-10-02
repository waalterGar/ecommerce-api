package com.waalterGar.projects.ecommerce.Dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
public class createOrderItemDto {
    @NotBlank(message = "productSku is required")
    private String productSku;

    @Positive(message = "quantity must be > 0")
    private Integer quantity;
}
