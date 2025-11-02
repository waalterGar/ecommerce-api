package com.waalterGar.projects.ecommerce.Dto;


import lombok.*;

@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequestDto {
    private String customerExternalId;
    private String note;
}
