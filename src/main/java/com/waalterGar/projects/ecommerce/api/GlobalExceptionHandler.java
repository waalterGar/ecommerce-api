package com.waalterGar.projects.ecommerce.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI TYPE_NOT_FOUND   = URI.create("urn:problem:not-found");
    private static final URI TYPE_INVALID     = URI.create("urn:problem:invalid-request");
    private static final URI TYPE_VALIDATION  = URI.create("urn:problem:validation");
    private static final URI TYPE_UNEXPECTED  = URI.create("urn:problem:unexpected");

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        return pd(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), TYPE_NOT_FOUND, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return pd(HttpStatus.BAD_REQUEST, "Invalid Request", ex.getMessage(), TYPE_INVALID, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ProblemDetail pd = pd(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more fields are invalid.",
                TYPE_VALIDATION,
                req
        );
        pd.setProperty("errors", fieldErrors(ex));
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolations(ConstraintViolationException ex, HttpServletRequest req) {
        ProblemDetail pd = pd(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "One or more parameters are invalid.",
                TYPE_VALIDATION,
                req
        );
        pd.setProperty("errors", parameterErrors(ex));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        // Keep details generic for 500s; log ex server-side if/when you add a logger.
        return pd(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected error.", TYPE_UNEXPECTED, req);
    }

    /** Build a ProblemDetail and enrich with common properties. */
    private static ProblemDetail pd(
            HttpStatus status,
            String title,
            String detail,
            URI type,
            HttpServletRequest req
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(type);
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }

    /** Map body field errors from Bean Validation into a simple field->message map. */
    private static Map<String, String> fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /** Map path/query param violations into a simple path->message map. */
    private static Map<String, String> parameterErrors(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}
