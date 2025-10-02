package com.waalterGar.projects.ecommerce.Dto;

import com.waalterGar.projects.ecommerce.utils.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class createOrderDto {
    @NotBlank(message = "customerExternalId is required")
    private String customerExternalId;

    @NotNull(message = "currency is required")
    private Currency currency;

    @NotEmpty(message = "items must not be empty")
    private List<createOrderItemDto> items;
}
