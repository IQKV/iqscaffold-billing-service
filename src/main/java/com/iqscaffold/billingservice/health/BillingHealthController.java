package com.iqscaffold.billingservice.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public health check endpoints for billing service modules.
 * Used by frontend to monitor service availability and display degradation banners.
 * 
 * <p>These endpoints are intentionally public (no authentication required) to allow
 * health monitoring even when authentication services are degraded.</p>
 */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing Health", description = "Public health check endpoints for billing service monitoring")
public class BillingHealthController {

  @Operation(
      summary = "Payments module health check",
      description = "Returns health status of the payments processing module")
  @ApiResponse(responseCode = "200", description = "Payments module is healthy")
  @GetMapping("/payments/health")
  public ResponseEntity<Map<String, Object>> paymentsHealth() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "module", "payments",
        "timestamp", Instant.now().toString()
    ));
  }

  @Operation(
      summary = "Subscriptions module health check",
      description = "Returns health status of the subscriptions management module")
  @ApiResponse(responseCode = "200", description = "Subscriptions module is healthy")
  @GetMapping("/subscriptions/health")
  public ResponseEntity<Map<String, Object>> subscriptionsHealth() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "module", "subscriptions",
        "timestamp", Instant.now().toString()
    ));
  }

  @Operation(
      summary = "Invoices module health check",
      description = "Returns health status of the invoicing module")
  @ApiResponse(responseCode = "200", description = "Invoices module is healthy")
  @GetMapping("/invoices/health")
  public ResponseEntity<Map<String, Object>> invoicesHealth() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "module", "invoices",
        "timestamp", Instant.now().toString()
    ));
  }

  @Operation(
      summary = "Payment gateway module health check",
      description = "Returns health status of the payment gateway integration module")
  @ApiResponse(responseCode = "200", description = "Payment gateway module is healthy")
  @GetMapping("/gateway/health")
  public ResponseEntity<Map<String, Object>> gatewayHealth() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "module", "gateway",
        "timestamp", Instant.now().toString()
    ));
  }
}
