package com.waalterGar.projects.ecommerce.Dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCartItemDto {
    @Min(value=1, message = "qty must be at least 1")
    private int qty;
}
