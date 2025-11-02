package com.waalterGar.projects.ecommerce.controller;

import com.waalterGar.projects.ecommerce.Dto.AddCartItemDto;
import com.waalterGar.projects.ecommerce.Dto.CartDto;
import com.waalterGar.projects.ecommerce.Dto.OrderDto;
import com.waalterGar.projects.ecommerce.Dto.UpdateCartItemDto;
import com.waalterGar.projects.ecommerce.service.CartService;
import com.waalterGar.projects.ecommerce.utils.Currency;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Carts", description = "Create, retrieve and modify carts")
@RequestMapping("/carts")
@RestController
@AllArgsConstructor
public class CartController {
    private final CartService cartService;

    @Operation(summary = "Create a new cart")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<CartDto> createCart(@RequestParam(required = false) Currency currency) {
        CartDto createdCart = cartService.createCart(currency);
        return new ResponseEntity<>(createdCart, HttpStatus.CREATED);
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<CartDto> getCart(@PathVariable("externalId") String externalId) {
        CartDto createdCart  = cartService.getCartByExternalId(externalId);
        return new ResponseEntity<>(createdCart, HttpStatus.OK);
    }

    @PostMapping("/{externalId}/items")
    public ResponseEntity<CartDto> addItem(@PathVariable("externalId") String externalId, @Valid @RequestBody AddCartItemDto body) {
        return ResponseEntity.ok(cartService.addItem(externalId, body.getSku(), body.getQty()));
    }

    @PutMapping("/{id}/items/{sku}")
    public ResponseEntity<CartDto> updateQty(@PathVariable("id") String id, @PathVariable String sku, @Valid @RequestBody UpdateCartItemDto body) {
        return ResponseEntity.ok(cartService.updateQty(id, sku, body.getQty()));
    }

    @DeleteMapping("/{externalId}/items/{sku}")
    public ResponseEntity<CartDto> removeItem(@PathVariable("externalId") String externalId, @PathVariable("sku")  String sku) {
        return ResponseEntity.ok(cartService.removeItem(externalId, sku));
    }

    @DeleteMapping("/{externalId}/items")
    public ResponseEntity<CartDto> clearCart(@PathVariable("externalId") String externalId) {
        return ResponseEntity.ok(cartService.clearCart(externalId));
    }
}
