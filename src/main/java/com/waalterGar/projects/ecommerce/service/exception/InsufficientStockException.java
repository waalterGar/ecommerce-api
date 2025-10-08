package com.waalterGar.projects.ecommerce.service.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException() {
        super("Insufficient stock for one or more items.");
    }
    public InsufficientStockException(String message) {
        super(message);
    }
}
