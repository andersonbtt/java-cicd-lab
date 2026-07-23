package com.bittclouds.labjavacicd.web;

import com.bittclouds.labjavacicd.task.TaskNotFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fields = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fields.put(error.getField(), error.getDefaultMessage());
    }

    log.warn("event=validation_failed fields={}", fields.keySet());
    return ResponseEntity.badRequest().body(errorBody(
        HttpStatus.BAD_REQUEST,
        "validation_failed",
        "Request validation failed",
        fields));
  }

  @ExceptionHandler(TaskNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(TaskNotFoundException ex) {
    log.warn("event=task_not_found taskId={}", ex.getTaskId());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(
        HttpStatus.NOT_FOUND,
        "task_not_found",
        ex.getMessage(),
        Map.of("taskId", ex.getTaskId())));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
    log.error("event=unhandled_error message={}", ex.getMessage(), ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal_error",
        "Unexpected server error",
        Map.of()));
  }

  private static Map<String, Object> errorBody(
      HttpStatus status,
      String code,
      String message,
      Map<String, ?> details) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("code", code);
    body.put("message", message);
    body.put("details", details);
    return body;
  }
}
