package com.springai.chatspringai.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.fasterxml.jackson.core.JsonProcessingException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * Gestionnaire global des exceptions REST.
     *
     * Centralise le format des réponses d'erreur pour les exceptions courantes
     * (validation, erreurs de parsing JSON, erreurs génériques).
     */

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> body = new HashMap<>();
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse(ex.getMessage());
        body.put("error", "Validation failed");
        body.put("message", message);
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, JsonProcessingException.class, IllegalArgumentException.class})
    public ResponseEntity<Object> handleBadRequest(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Bad request");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "Internal server error");
        body.put("message", "Une erreur interne est survenue.");
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
