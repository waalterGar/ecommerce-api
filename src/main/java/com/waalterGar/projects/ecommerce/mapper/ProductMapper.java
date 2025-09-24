package com.waalterGar.projects.ecommerce.mapper;

import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.entity.Product;

public class ProductMapper {
    public static ProductDto toDto(Product product) {
        return new ProductDto(
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getStockQuantity(),
                product.getIsActive()
        );
    }

    public static Product toEntity(ProductDto productDto) {
       return new Product(
                productDto.getSku(),
                productDto.getName(),
                productDto.getDescription(),
                productDto.getPrice(),
                productDto.getCurrency(),
                productDto.getStockQuantity(),
               productDto.getIsActive()
       );
    }
}
