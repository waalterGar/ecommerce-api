package com.waalterGar.projects.ecommerce.api;

import com.waalterGar.projects.ecommerce.api.problem.InvalidPaginationException;
import com.waalterGar.projects.ecommerce.api.problem.InvalidSortException;
import com.waalterGar.projects.ecommerce.service.exception.InactiveProductException;
import com.waalterGar.projects.ecommerce.service.exception.InsufficientStockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.UnexpectedTypeException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI TYPE_NOT_FOUND       = URI.create("urn:problem:not-found");
    private static final URI TYPE_INVALID = URI.create("urn:problem:invalid-request");
    private static final URI TYPE_VALIDATION      = URI.create("urn:problem:validation");
    private static final URI TYPE_MALFORMED_JSON  = URI.create("urn:problem:malformed-json");
    private static final URI TYPE_TYPE_MISMATCH   = URI.create("urn:problem:type-mismatch");
    private static final URI TYPE_MISSING_PARAM   = URI.create("urn:problem:missing-param");
    private static final URI TYPE_NO_RESOURCE     = URI.create("urn:problem:no-resource");
    private static final URI TYPE_UNEXPECTED      = URI.create("urn:problem:unexpected");
    private static final URI TYPE_UNSUPPORTED_MEDIA = URI.create("urn:problem:unsupported-media-type");
    private static final URI TYPE_NOT_ACCEPTABLE    = URI.create("urn:problem:not-acceptable");
    private static final URI TYPE_INSUFFICIENT_STOCK = URI.create("urn:problem:insufficient-stock");


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

    @ExceptionHandler(UnexpectedTypeException.class)
    public ProblemDetail handleUnexpectedType(UnexpectedTypeException ex, HttpServletRequest req) {
         return pd(
                HttpStatus.BAD_REQUEST,
                "Invalid Constraint Configuration",
                "A server configuration error occurred.",
                TYPE_VALIDATION,
                req
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String detail = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage()
                : "Malformed JSON request body.";
        return pd(HttpStatus.BAD_REQUEST, "Malformed JSON", detail, TYPE_MALFORMED_JSON, req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        String param = ex.getName();
        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String detail = "Parameter '%s' should be of type '%s'.".formatted(param, expected);
        return pd(HttpStatus.BAD_REQUEST, "Type Mismatch", detail, TYPE_TYPE_MISMATCH, req);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest req) {
        String detail = "Missing required parameter '%s' of type '%s'.".formatted(ex.getParameterName(), ex.getParameterType());
        return pd(HttpStatus.BAD_REQUEST, "Missing Parameter", detail, TYPE_MISSING_PARAM, req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return pd(HttpStatus.NOT_FOUND, "No Resource Found", ex.getMessage(), TYPE_NO_RESOURCE, req);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return pd(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type", ex.getMessage(), TYPE_UNSUPPORTED_MEDIA, req);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ProblemDetail handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest req) {
        return pd(HttpStatus.NOT_ACCEPTABLE, "Not Acceptable", ex.getMessage(), TYPE_NOT_ACCEPTABLE, req);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex, HttpServletRequest req) {
        return pd(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Stock", ex.getMessage(), TYPE_INSUFFICIENT_STOCK, req);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        return pd(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Unexpected error.", TYPE_UNEXPECTED, req);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return pd(HttpStatus.CONFLICT, "Optimistic Lock Conflict", "Concurrent update conflict. Please retry.", URI.create("urn:problem:conflict"), req);
    }

    @ExceptionHandler(InactiveProductException.class)
    public ProblemDetail handleInactiveProduct(InactiveProductException ex, HttpServletRequest req) {
        return pd(HttpStatus.UNPROCESSABLE_ENTITY,"Inactive Product", ex.getMessage(), URI.create("urn:problem:inactive-product"), req);
    }

    @ExceptionHandler(InvalidPaginationException.class)
    public ProblemDetail handleInvalidPagination(InvalidPaginationException ex, HttpServletRequest req) {
        return pd(HttpStatus.BAD_REQUEST, "Invalid pagination parameters", ex.getMessage(), URI.create("urn:problem:invalid-pagination"), req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return pd(HttpStatus.BAD_REQUEST, "Invalid state", ex.getMessage(), TYPE_INVALID, req);
    }

    @ExceptionHandler(InvalidSortException.class)
    public ProblemDetail handleInvalidSort(InvalidSortException ex, HttpServletRequest req) {
        ProblemDetail problem = pd(
                HttpStatus.BAD_REQUEST,
                "Invalid sort parameter",
                ex.getMessage(),
                URI.create("urn:problem:invalid-sort"),
                req
        );
        if (ex.field() != null) problem.setProperty("invalidField", ex.field());
        if (ex.allowed() != null) problem.setProperty("allowedFields", ex.allowed());
        return problem;
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
