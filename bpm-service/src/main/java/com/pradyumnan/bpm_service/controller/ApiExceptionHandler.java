package com.pradyumnan.bpm_service.controller;

import com.pradyumnan.bpm_service.service.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** Turns errors into the {"error": "..."} body the frontend already knows how to show. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
    }

    // Someone else changed the same case between your page loading and your click
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleConflict(OptimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Someone else changed this case. Refresh and try again."));
    }

    // ai-service rejected the file itself (for example, a fake image)
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<Map<String, String>> handleAiClientError(HttpClientErrorException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "The uploaded file could not be processed as a valid PNG/JPEG image"));
    }

    // ai-service is down, or failed
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleAiUnavailable(RestClientException e) {
        logger.error("Analysis service call failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "The analysis service is unavailable. Try again shortly."));
    }
}
