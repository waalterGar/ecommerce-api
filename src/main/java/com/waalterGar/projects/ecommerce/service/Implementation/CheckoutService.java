package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;

public interface CheckoutService {
    OrderDto checkout(String cartExternalId, String customerExternalId);
}
