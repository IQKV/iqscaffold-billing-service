package com.iqscaffold.billingservice.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class FeatureInternalResourceTest {

  @Mock
  private FeatureEnablementService featureEnablementService;

  @Mock
  private FeatureUsageTrackingService usageTrackingService;

  @InjectMocks
  private FeatureInternalResource internalResource;

  private String tenantId;
  private String featureKey;
  private FeatureContext mockContext;

  @BeforeEach
  void setUp() {
    tenantId = "tenant-123";
    featureKey = "advanced_analytics";

    mockContext = FeatureContext.builder(tenantId)
        .planId("plan-456")
        .planName("Pro Plan")
        .enabledFeatures(Set.of("advanced_analytics", "api_access"))
        .quotas(Map.of("api_calls_monthly", 50000L))
        .build();
  }

  @Test
  @DisplayName("Should get feature context for tenant")
  void shouldGetFeatureContextForTenant() {
    // Arrange
    when(featureEnablementService.getFeatureContext(tenantId))
        .thenReturn(mockContext);

    // Act
    var response = internalResource.getFeatureContext(tenantId);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTenantId()).isEqualTo(tenantId);
    assertThat(response.getBody().getPlanName()).isEqualTo("Pro Plan");
  }

  @Test
  @DisplayName("Should return empty context on error")
  void shouldReturnEmptyContextOnError() {
    // Arrange
    when(featureEnablementService.getFeatureContext(tenantId))
        .thenThrow(new RuntimeException("Service error"));

    // Act
    var response = internalResource.getFeatureContext(tenantId);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTenantId()).isEqualTo(tenantId);
    assertThat(response.getBody().hasAnyFeatures()).isFalse();
  }

  @Test
  @DisplayName("Should record feature usage without metadata")
  void shouldRecordFeatureUsageWithoutMetadata() {
    // Arrange
    var usageRequest = new FeatureInternalResource.FeatureUsageRequest(
        tenantId, featureKey, "/api/v1/analytics"
    );

    // Act
    var response = internalResource.recordFeatureUsage(usageRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(usageTrackingService).recordUsage(tenantId, featureKey, "/api/v1/analytics");
  }

  @Test
  @DisplayName("Should record feature usage with metadata")
  void shouldRecordFeatureUsageWithMetadata() {
    // Arrange
    var usageRequest = new FeatureInternalResource.FeatureUsageRequest(
        tenantId, featureKey, "/api/v1/analytics"
    );
    usageRequest.setUserId("user-789");
    usageRequest.setCorrelationId("corr-012");

    // Act
    var response = internalResource.recordFeatureUsage(usageRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(usageTrackingService).recordUsage(
        eq(tenantId),
        eq(featureKey),
        eq("/api/v1/analytics"),
        anyMap()
    );
  }

  @Test
  @DisplayName("Should not fail when usage tracking fails")
  void shouldNotFailWhenUsageTrackingFails() {
    // Arrange
    var usageRequest = new FeatureInternalResource.FeatureUsageRequest(
        tenantId, featureKey, "/api/v1/analytics"
    );
    doThrow(new RuntimeException("Tracking failed"))
        .when(usageTrackingService).recordUsage(anyString(), anyString(), anyString());

    // Act
    var response = internalResource.recordFeatureUsage(usageRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  @DisplayName("Should check if feature is enabled")
  void shouldCheckIfFeatureIsEnabled() {
    // Arrange
    when(featureEnablementService.isFeatureEnabled(tenantId, featureKey))
        .thenReturn(true);

    // Act
    var response = internalResource.isFeatureEnabled(tenantId, featureKey);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isEnabled()).isTrue();
    assertThat(response.getBody().getFeatureKey()).isEqualTo(featureKey);
    assertThat(response.getBody().getTenantId()).isEqualTo(tenantId);
  }

  @Test
  @DisplayName("Should return false when feature is not enabled")
  void shouldReturnFalseWhenFeatureIsNotEnabled() {
    // Arrange
    when(featureEnablementService.isFeatureEnabled(tenantId, featureKey))
        .thenReturn(false);

    // Act
    var response = internalResource.isFeatureEnabled(tenantId, featureKey);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isEnabled()).isFalse();
  }

  @Test
  @DisplayName("Should return false on error when checking feature enablement")
  void shouldReturnFalseOnErrorWhenCheckingFeatureEnablement() {
    // Arrange
    when(featureEnablementService.isFeatureEnabled(tenantId, featureKey))
        .thenThrow(new RuntimeException("Service error"));

    // Act
    var response = internalResource.isFeatureEnabled(tenantId, featureKey);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().isEnabled()).isFalse();
  }

  @Test
  @DisplayName("Should create FeatureUsageRequest with all fields")
  void shouldCreateFeatureUsageRequestWithAllFields() {
    // Arrange & Act
    var request = new FeatureInternalResource.FeatureUsageRequest(
        tenantId, featureKey, "/api/v1/test"
    );
    request.setUserId("user-123");
    request.setCorrelationId("corr-456");

    // Assert
    assertThat(request.getTenantId()).isEqualTo(tenantId);
    assertThat(request.getFeatureKey()).isEqualTo(featureKey);
    assertThat(request.getEndpoint()).isEqualTo("/api/v1/test");
    assertThat(request.getUserId()).isEqualTo("user-123");
    assertThat(request.getCorrelationId()).isEqualTo("corr-456");
  }

  @Test
  @DisplayName("Should create FeatureEnabledResponse with all fields")
  void shouldCreateFeatureEnabledResponseWithAllFields() {
    // Arrange & Act
    var response = new FeatureInternalResource.FeatureEnabledResponse(
        true, featureKey, tenantId
    );

    // Assert
    assertThat(response.isEnabled()).isTrue();
    assertThat(response.getFeatureKey()).isEqualTo(featureKey);
    assertThat(response.getTenantId()).isEqualTo(tenantId);
  }

  @Test
  @DisplayName("Should handle usage request with only correlation ID")
  void shouldHandleUsageRequestWithOnlyCorrelationId() {
    // Arrange
    var usageRequest = new FeatureInternalResource.FeatureUsageRequest(
        tenantId, featureKey, "/api/v1/analytics"
    );
    usageRequest.setCorrelationId("corr-012");

    // Act
    var response = internalResource.recordFeatureUsage(usageRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(usageTrackingService).recordUsage(
        eq(tenantId),
        eq(featureKey),
        eq("/api/v1/analytics"),
        anyMap()
    );
  }

  @Test
  @DisplayName("Should handle usage request with only user ID")
  void shouldHandleUsageRequestWithOnlyUserId() {
    // Arrange
    var usageRequest = new FeatureInternalResource.FeatureUsageRequest(
        tenantId, featureKey, "/api/v1/analytics"
    );
    usageRequest.setUserId("user-789");

    // Act
    var response = internalResource.recordFeatureUsage(usageRequest);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(usageTrackingService).recordUsage(
        eq(tenantId),
        eq(featureKey),
        eq("/api/v1/analytics"),
        anyMap()
    );
  }
}
