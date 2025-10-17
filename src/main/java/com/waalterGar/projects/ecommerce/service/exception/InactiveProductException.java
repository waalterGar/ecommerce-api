package com.waalterGar.projects.ecommerce.service.exception;

public class InactiveProductException extends RuntimeException {
    public InactiveProductException(String message) {
        super(message);
    }
}
