package com.waalterGar.projects.ecommerce.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivationProductDto {
    @NotNull
    private Boolean isActive;
}
