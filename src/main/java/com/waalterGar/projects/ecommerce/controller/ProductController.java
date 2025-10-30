package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.ActivationProductDto;
import com.waalterGar.projects.ecommerce.Dto.ProductDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateProductDto;
import com.waalterGar.projects.ecommerce.api.pagination.*;
import com.waalterGar.projects.ecommerce.api.problem.InvalidPaginationException;
import com.waalterGar.projects.ecommerce.config.PaginationProperties;
import com.waalterGar.projects.ecommerce.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Products", description = "Manage products")

@RequestMapping("/products")
@RestController
public class ProductController {
    private final ProductService productService;
    private final AllowedSorts productsAllowedSorts;   // Provided by ProductSortConfig
    private final PaginationProperties props;

    public ProductController(ProductService service,
                             @Qualifier("productsAllowedSorts") AllowedSorts productsAllowedSorts,
                             PaginationProperties props) {
        this.productService = service;
        this.productsAllowedSorts = productsAllowedSorts;
        this.props = props;
    }

    @Operation(summary = "List products (paged)")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageEnvelope<ProductDto>> listProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) Integer size,
            @RequestParam MultiValueMap<String,String> query
    ) {
        int effectiveSize = (size == null) ? props.defaultSize() : size;
        if (effectiveSize < 1 || effectiveSize > props.maxSize()) {
            throw new InvalidPaginationException("size must be between 1 and " + props.maxSize());
        }

        List<String> sortRaw = query.get("sort");
        List<SortDirective> directives = SortParser.parse(sortRaw);

        SortValidator.ensureAllowed(directives, productsAllowedSorts);

        Pageable pageable = PageableFactory.from(
                page,
                effectiveSize,
                directives,
                productsAllowedSorts,
                props.defaults().products(),
                props
        );

        PageEnvelope<ProductDto> body = productService.list(pageable);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct (@RequestBody ProductDto productDto) {
        ProductDto createdProduct = productService.createProduct(productDto);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @Operation(summary = "Get product by SKU")
    @GetMapping("/{sku}")
    public ResponseEntity<ProductDto> getProductBySku(@PathVariable String sku) {
        ProductDto product = productService.getProductBySku(sku);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @PutMapping( path = "/{sku}",
            consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
            produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ProductDto> updateProduct(@PathVariable("sku") String sku,@Valid @RequestBody UpdateProductDto dto){
        ProductDto updatedProductDto = productService.updateProduct(sku, dto);
        return ResponseEntity.ok(updatedProductDto);
    }

    @PatchMapping(path = "/{sku}/activation",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductDto> setActivation(@PathVariable String sku, @Valid @RequestBody ActivationProductDto dto) {
        return ResponseEntity.ok(productService.setProductActive(sku, dto));
    }
}
