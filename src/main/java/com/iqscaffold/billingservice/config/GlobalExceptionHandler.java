package com.iqscaffold.billingservice.config;

import java.net.URI;
import java.time.Instant;

import com.iqscaffold.billingservice.payout.PayoutNotFoundException;
import com.iqscaffold.billingservice.shared.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final MessageService messageService;

  public GlobalExceptionHandler(final MessageService messageService) {
    this.messageService = messageService;
  }

  @ExceptionHandler(PayoutNotFoundException.class)
  ProblemDetail handlePayoutNotFound(PayoutNotFoundException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        e.getMessage()
    );
    problemDetail.setTitle("Payout Not Found");
    problemDetail.setType(URI.create("urn:problem-type:payout-not-found"));
    problemDetail.setProperty("timestamp", Instant.now());
    logger.warn("Payout not found: {}", e.getMessage());
    return problemDetail;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnhandled(Exception e) {
    // Log the full exception with stack trace for debugging
    logger.error("Unexpected error occurred in billing service: {}", e.getMessage(), e);
    
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        messageService.getMessage("error.unexpected")
    );
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setType(URI.create("urn:problem-type:internal-server-error"));
    problemDetail.setProperty("timestamp", Instant.now());
    problemDetail.setProperty("exceptionType", e.getClass().getSimpleName());
    return problemDetail;
  }
}
