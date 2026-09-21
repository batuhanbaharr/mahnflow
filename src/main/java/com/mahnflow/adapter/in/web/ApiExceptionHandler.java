package com.mahnflow.adapter.in.web;

import com.mahnflow.application.service.InvoiceNotFound;
import com.mahnflow.domain.invoice.InvalidStateChange;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

// turns our exceptions into clean http answers with the right status code.
// without this every error would just be a 500, which is not very helpful. one
// @RestControllerAdvice catches them in one place — learned this trick from a
// baeldung article on spring exception handling.
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvoiceNotFound.class)
    public ResponseEntity<Map<String, Object>> notFound(InvoiceNotFound e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // a wrong state move (like paying a draft) is a conflict, not a server error.
    @ExceptionHandler(InvalidStateChange.class)
    public ResponseEntity<Map<String, Object>> badState(InvalidStateChange e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    // two updates raced on the same invoice and one lost. ask the client to retry.
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> locked(OptimisticLockingFailureException e) {
        return build(HttpStatus.CONFLICT, "invoice was changed by someone else, try again");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .orElse("bad request");
        return build(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "time", Instant.now().toString(),
                "status", status.value(),
                "error", message));
    }
}
