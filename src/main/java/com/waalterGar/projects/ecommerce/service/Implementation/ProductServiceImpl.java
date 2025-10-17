package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateProductDto;
import com.waalterGar.projects.ecommerce.entity.Product;
import com.waalterGar.projects.ecommerce.mapper.ProductMapper;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.service.ProductService;
import com.waalterGar.projects.ecommerce.utils.Currency;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    @Override
    public List<ProductDto> getAllProducts() {
       List<Product> products = productRepository.findAll();
       return products.stream().map((product) -> ProductMapper.toDto(product)).collect(Collectors.toList());
    }

    @Override
    public ProductDto createProduct(ProductDto productDto) {
        Product product = ProductMapper.toEntity(productDto);
        Product savedProduct = productRepository.save(product);
        return ProductMapper.toDto(savedProduct);
    }

    @Override
    public ProductDto getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));
        return ProductMapper.toDto(product);
    }

    @Transactional
    @Override
    public ProductDto updateProduct(String sku, UpdateProductDto dto) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("Invalid sku");
        }

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new NoSuchElementException("Product not found"));

        product.setName(dto.getName().trim());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice().setScale(2, RoundingMode.HALF_UP));
        product.setStockQuantity(dto.getStockQuantity());
        product.setIsActive(dto.getIsActive());

        return ProductMapper.toDto(product);
    }
}