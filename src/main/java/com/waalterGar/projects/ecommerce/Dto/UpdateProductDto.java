package com.waalterGar.projects.ecommerce.Dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductDto {
    @NotBlank(message = "name is required")
    private String name;

    @Size(max = 1000, message = "description must be at most 1000 characters")
    private String description;

    @NotNull(message = "price is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "price must be >= 0.00")
    @Digits(integer = 12, fraction = 2, message = "price must have up to 12 integer digits and 2 decimals")
    private BigDecimal price;

    @NotNull(message = "stockQuantity is required")
    @Min(value = 0, message = "stockQuantity must be >= 0")
    private Integer stockQuantity;

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
