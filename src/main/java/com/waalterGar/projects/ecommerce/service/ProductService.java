package com.waalterGar.projects.ecommerce.service;

import com.waalterGar.projects.ecommerce.Dto.ProductDto;

import java.util.List;

public interface ProductService {
    List<ProductDto> getAllProducts();

    ProductDto createProduct(ProductDto productDto);

    ProductDto getProductBySku(String sku);
}
