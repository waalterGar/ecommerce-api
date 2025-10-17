package com.waalterGar.projects.ecommerce.service;

import com.waalterGar.projects.ecommerce.Dto.ActivationProductDto;
import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateProductDto;
import jakarta.validation.Valid;

import java.util.List;

public interface ProductService {
    List<ProductDto> getAllProducts();

    ProductDto createProduct(ProductDto productDto);

    ProductDto getProductBySku(String sku);

    ProductDto updateProduct(String sku, UpdateProductDto dto);

    ProductDto setProductActive(String sku, @Valid ActivationProductDto dto);
}
