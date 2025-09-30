package com.waalterGar.projects.ecommerce.service;


import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;

import java.util.List;

public interface OrderService {
    OrderDto createOrder(createOrderDto orderDto);
    List<OrderDto> getAllOrders();
    OrderDto getOrderByExternalId(String orderNumber);
}