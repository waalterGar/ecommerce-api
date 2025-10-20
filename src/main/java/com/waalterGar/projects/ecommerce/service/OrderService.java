package com.waalterGar.projects.ecommerce.service;


import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.PayOrderRequestDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;

import java.util.List;

public interface OrderService {
    OrderDto createOrder(createOrderDto orderDto);
    List<OrderDto> getAllOrders();
    OrderDto getOrderByExternalId(String orderNumber);
    OrderDto pay(String externalId, PayOrderRequestDto dto);
}