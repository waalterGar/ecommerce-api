package com.waalterGar.projects.ecommerce.service;

import com.waalterGar.projects.ecommerce.Dto.ActivationProductDto;
import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateProductDto;
import com.waalterGar.projects.ecommerce.api.pagination.PageEnvelope;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductService {
    List<ProductDto> getAllProducts();

    ProductDto createProduct(ProductDto productDto);

    ProductDto getProductBySku(String sku);

    ProductDto updateProduct(String sku, UpdateProductDto dto);

    ProductDto setProductActive(String sku, @Valid ActivationProductDto dto);

    PageEnvelope<ProductDto> list(Pageable pageable);
}
