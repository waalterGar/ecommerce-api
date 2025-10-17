package com.waalterGar.projects.ecommerce.service.Implementation;

import com.waalterGar.projects.ecommerce.Dto.ActivationProductDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateProductDto;
import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.entity.Product;
import com.waalterGar.projects.ecommerce.repository.ProductRepository;
import com.waalterGar.projects.ecommerce.utils.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    // Defaults used across tests (kept simple)
    private static final String DEFAULT_SKU = "SKU123";
    private static final String DEFAULT_NAME = "Test Product";
    private static final String DEFAULT_DESCRIPTION = "This is a test product";
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("19.99");
    private static final Currency DEFAULT_CURRENCY = Currency.EUR;
    private static final Integer DEFAULT_STOCK = 10;
    private static final Boolean DEFAULT_ACTIVE = true;

    @Test
    @DisplayName("createProduct: saves and returns ProductDto")
    void createProduct_withValidDto_returnsProductDto() {
        // Given
        ProductDto input = createDefaultProductDto();
        Product saved = createDefaultProductEntity(); // service will map entity -> dto using real mapper
        saved.setId(UUID.randomUUID());
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        // When
        ProductDto result = productService.createProduct(input);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo(DEFAULT_SKU);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("getProductBySku: returns ProductDto when SKU exists")
    void getProductBySku_withExistingSku_returnsProductDto() {
        // Given
        Product entity = createDefaultProductEntity();
        entity.setId(UUID.randomUUID());
        when(productRepository.findBySku(eq(DEFAULT_SKU))).thenReturn(Optional.of(entity));

        // When
        ProductDto result = productService.getProductBySku(DEFAULT_SKU);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo(DEFAULT_SKU);
        verify(productRepository).findBySku(eq(DEFAULT_SKU));
    }

    @Test
    @DisplayName("getProductBySku: throws NoSuchElementException when SKU does not exist")
    void getProductBySku_withNonExistentSku_throwsNoSuchElementException() {
        // Given
        String missing = "MISSING-SKU";
        when(productRepository.findBySku(eq(missing))).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> productService.getProductBySku(missing))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("Product not found");
        verify(productRepository).findBySku(eq(missing));
    }

    @Test
    @DisplayName("getAllProducts: returns empty list when repository has no products")
    void getAllProducts_withNoProducts_returnsEmptyList() {
        // Given
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<ProductDto> result = productService.getAllProducts();

        // Then
        assertThat(result).isNotNull().isEmpty();
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("updateProduct: updates managed entity, trims name, scales price(2), returns ProductDto")
    void updateProduct_withValidDto_updatesEntityAndReturnsDto() {
        // Given
        Product existing = createDefaultProductEntity();
        when(productRepository.findBySku(eq(DEFAULT_SKU))).thenReturn(Optional.of(existing));

        UpdateProductDto dto = new UpdateProductDto(
                "  New Name  ",                 // will be trimmed
                "Updated description",
                new BigDecimal("10.995"),       // will be scaled to 11.00 (HALF_UP)
                25,
                false
        );

        // When
        ProductDto out = productService.updateProduct(DEFAULT_SKU, dto);

        // Then (returned DTO reflects updates)
        assertThat(out).isNotNull();
        assertThat(out.getSku()).isEqualTo(DEFAULT_SKU);      // identity unchanged
        assertThat(out.getName()).isEqualTo("New Name");      // trimmed
        assertThat(out.getDescription()).isEqualTo("Updated description");
        assertThat(out.getPrice()).isEqualByComparingTo(new BigDecimal("11.00")); // scaled(2)
        assertThat(out.getStockQuantity()).isEqualTo(25);
        assertThat(out.getIsActive()).isFalse();

        // And (managed entity was mutated inside the transaction)
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getPrice()).isEqualByComparingTo("11.00");
        assertThat(existing.getStockQuantity()).isEqualTo(25);
        assertThat(existing.getIsActive()).isFalse();

        verify(productRepository).findBySku(eq(DEFAULT_SKU));
        verifyNoMoreInteractions(productRepository); // relying on JPA dirty checking (no save)
    }

    @Test
    @DisplayName("updateProduct: throws NoSuchElementException when SKU does not exist")
    void updateProduct_withNonExistentSku_throwsNoSuchElementException() {
        // Given
        String missing = "MISSING-SKU";
        when(productRepository.findBySku(eq(missing))).thenReturn(Optional.empty());

        UpdateProductDto dto = new UpdateProductDto(
                "Name", "Desc", new BigDecimal("1.00"), 1, true
        );

        // When / Then
        assertThatThrownBy(() -> productService.updateProduct(missing, dto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Product not found");
        verify(productRepository).findBySku(eq(missing));
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("updateProduct: throws IllegalArgumentException when SKU is blank")
    void updateProduct_withBlankSku_throwsIllegalArgumentException() {
        UpdateProductDto dto = new UpdateProductDto(
                "Name", "Desc", new BigDecimal("1.00"), 1, true
        );

        assertThatThrownBy(() -> productService.updateProduct("   ", dto))
                .isInstanceOf(IllegalArgumentException.class);
        // repo should not be called
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("setProductActive: flips flag on managed entity and returns updated DTO")
    void setProductActive_happyPath_updatesEntityAndReturnsDto() {
        // Given
        Product existing = createDefaultProductEntity();
        existing.setIsActive(false); // initial state
        when(productRepository.findBySku(eq(DEFAULT_SKU))).thenReturn(Optional.of(existing));

        ActivationProductDto dto = new ActivationProductDto(true);

        // When
        ProductDto out = productService.setProductActive(DEFAULT_SKU, dto);

        // Then (DTO reflects new state)
        assertThat(out).isNotNull();
        assertThat(out.getSku()).isEqualTo(DEFAULT_SKU);
        assertThat(out.getIsActive()).isTrue();

        // And (entity actually mutated; dirty checking will persist in real tx)
        assertThat(existing.getIsActive()).isTrue();

        verify(productRepository).findBySku(eq(DEFAULT_SKU));
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("setProductActive: throws NoSuchElementException when SKU does not exist")
    void setProductActive_notFound_throwsNoSuchElementException() {
        String missing = "MISSING-SKU";
        when(productRepository.findBySku(eq(missing))).thenReturn(Optional.empty());

        ActivationProductDto dto = new ActivationProductDto(true);

        assertThatThrownBy(() -> productService.setProductActive(missing, dto))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository).findBySku(eq(missing));
        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("setProductActive: throws IllegalArgumentException when SKU is blank")
    void setProductActive_blankSku_throwsIllegalArgumentException() {
        ActivationProductDto dto = new ActivationProductDto(true);

        assertThatThrownBy(() -> productService.setProductActive("   ", dto))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoMoreInteractions(productRepository);
    }

    @Test
    @DisplayName("setProductActive: throws IllegalArgumentException when body or flag is null")
    void setProductActive_nullBodyOrFlag_throwsIllegalArgumentException() {
        // null body
        assertThatThrownBy(() -> productService.setProductActive(DEFAULT_SKU, null))
                .isInstanceOf(IllegalArgumentException.class);

        // null flag
        ActivationProductDto bad = new ActivationProductDto(null);
        assertThatThrownBy(() -> productService.setProductActive(DEFAULT_SKU, bad))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoMoreInteractions(productRepository);
    }
    // ---------- Helpers ----------

    private Product createDefaultProductEntity() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setSku(DEFAULT_SKU);
        product.setName(DEFAULT_NAME);
        product.setDescription(DEFAULT_DESCRIPTION);
        product.setPrice(DEFAULT_PRICE);
        product.setCurrency(DEFAULT_CURRENCY);
        product.setStockQuantity(DEFAULT_STOCK);
        product.setIsActive(DEFAULT_ACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        return product;
    }

    private ProductDto createDefaultProductDto() {
        ProductDto dto = new ProductDto();
        dto.setSku(DEFAULT_SKU);
        dto.setName(DEFAULT_NAME);
        dto.setDescription(DEFAULT_DESCRIPTION);
        dto.setPrice(DEFAULT_PRICE);
        dto.setCurrency(DEFAULT_CURRENCY);
        dto.setStockQuantity(DEFAULT_STOCK);
        dto.setIsActive(DEFAULT_ACTIVE);
        return dto;
    }
}
