package com.waalterGar.projects.ecommerce.api.problem;

public class InvalidPaginationException extends RuntimeException {
  public InvalidPaginationException(String message) {
    super(message);
  }
}