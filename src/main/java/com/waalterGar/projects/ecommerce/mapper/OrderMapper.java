package com.waalterGar.projects.ecommerce.mapper;

import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.OrderItemDto;
import com.waalterGar.projects.ecommerce.Dto.createOrderDto;
import com.waalterGar.projects.ecommerce.entity.Order;
import com.waalterGar.projects.ecommerce.entity.OrderItem;
import com.waalterGar.projects.ecommerce.utils.OrderStatus;

import java.util.stream.Collectors;

public class OrderMapper {
    public static OrderDto toDto(Order order) {
        if (order == null) {
            return null;
        }
        OrderDto dto = new OrderDto();
        dto.setExternalId(order.getExternalId());
        dto.setCustomerExternalId(order.getCustomer().getExternalId());
        dto.setStatus(order.getStatus());
        dto.setCurrency(order.getCurrency());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        dto.setItems(order.getItems().stream()
                .map(OrderMapper::toItemDto)
                .collect(Collectors.toList()));
        return dto;
    }

    public static Order toEntity(OrderDto orderDto) {
        if (orderDto == null) {
            return null;
        }
        Order order = new Order();
        order.setExternalId(orderDto.getExternalId());
        order.setStatus(orderDto.getStatus());
        order.setCurrency(orderDto.getCurrency());
        order.setTotalAmount(orderDto.getTotalAmount());
        order.setCreatedAt(orderDto.getCreatedAt());
        order.setUpdatedAt(orderDto.getUpdatedAt());

        order.setItems(orderDto.getItems().stream()
                .map(OrderMapper::toItemEntity)
                .collect(Collectors.toList()));
        return order;
    }


    private static OrderItemDto toItemDto(OrderItem item) {
        if (item == null) return null;
        OrderItemDto dto = new OrderItemDto();
        dto.setProductSku(item.getProductSku());
        dto.setProductName(item.getProductName());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setCurrency(item.getCurrency());
        dto.setLineTotal(item.getLineTotal());
        return dto;
    }

    private static OrderItem toItemEntity(OrderItemDto itemDto) {
        if (itemDto == null) return null;
        OrderItem item = new OrderItem();
        item.setProductSku(itemDto.getProductSku());
        item.setProductName(itemDto.getProductName());
        item.setQuantity(itemDto.getQuantity());
        item.setUnitPrice(itemDto.getUnitPrice());
        item.setCurrency(itemDto.getCurrency());
        item.setLineTotal(itemDto.getLineTotal());
        return item;
    }

    public static Order fromCreateDto(createOrderDto dto) {
        if (dto == null) {return null;}

        return new Order();
    }
}
