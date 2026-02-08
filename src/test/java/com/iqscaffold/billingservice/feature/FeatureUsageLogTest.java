package com.iqscaffold.billingservice.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeatureUsageLogTest {

  @Test
  @DisplayName("Should create feature usage log with basic constructor")
  void shouldCreateFeatureUsageLogWithBasicConstructor() {
    // Arrange & Act
    var log = new FeatureUsageLog("tenant-123", "advanced_analytics", "/api/v1/analytics");

    // Assert
    assertThat(log.getTenantId()).isEqualTo("tenant-123");
    assertThat(log.getFeatureKey()).isEqualTo("advanced_analytics");
    assertThat(log.getEndpoint()).isEqualTo("/api/v1/analytics");
  }

  @Test
  @DisplayName("Should create feature usage log with user ID constructor")
  void shouldCreateFeatureUsageLogWithUserIdConstructor() {
    // Arrange & Act
    var log = new FeatureUsageLog("tenant-123", "advanced_analytics", "/api/v1/analytics", "user-456");

    // Assert
    assertThat(log.getTenantId()).isEqualTo("tenant-123");
    assertThat(log.getFeatureKey()).isEqualTo("advanced_analytics");
    assertThat(log.getEndpoint()).isEqualTo("/api/v1/analytics");
    assertThat(log.getUserId()).isEqualTo("user-456");
  }

  @Test
  @DisplayName("Should set timestamp on persist")
  void shouldSetTimestampOnPersist() {
    // Arrange
    var log = new FeatureUsageLog("tenant-123", "advanced_analytics", "/api/v1/analytics");

    // Act
    log.onCreate();

    // Assert
    assertThat(log.getTimestamp()).isNotNull();
    assertThat(log.getTimestamp()).isBeforeOrEqualTo(Instant.now());
  }

  @Test
  @DisplayName("Should not override existing timestamp on persist")
  void shouldNotOverrideExistingTimestampOnPersist() {
    // Arrange
    var log = new FeatureUsageLog("tenant-123", "advanced_analytics", "/api/v1/analytics");
    var existingTimestamp = Instant.now().minusSeconds(3600);
    log.setTimestamp(existingTimestamp);

    // Act
    log.onCreate();

    // Assert
    assertThat(log.getTimestamp()).isEqualTo(existingTimestamp);
  }

  @Test
  @DisplayName("Should set and get all properties")
  void shouldSetAndGetAllProperties() {
    // Arrange
    var log = new FeatureUsageLog();
    var timestamp = Instant.now();
    var metadata = Map.<String, Object>of("key", "value");

    // Act
    log.setId(1L);
    log.setTenantId("tenant-123");
    log.setFeatureKey("advanced_analytics");
    log.setEndpoint("/api/v1/analytics");
    log.setUserId("user-456");
    log.setTimestamp(timestamp);
    log.setSessionId("session-789");
    log.setCorrelationId("corr-012");
    log.setMetadata(metadata);

    // Assert
    assertThat(log.getId()).isEqualTo(1L);
    assertThat(log.getTenantId()).isEqualTo("tenant-123");
    assertThat(log.getFeatureKey()).isEqualTo("advanced_analytics");
    assertThat(log.getEndpoint()).isEqualTo("/api/v1/analytics");
    assertThat(log.getUserId()).isEqualTo("user-456");
    assertThat(log.getTimestamp()).isEqualTo(timestamp);
    assertThat(log.getSessionId()).isEqualTo("session-789");
    assertThat(log.getCorrelationId()).isEqualTo("corr-012");
    assertThat(log.getMetadata()).isEqualTo(metadata);
  }

  @Test
  @DisplayName("Should implement equals correctly")
  void shouldImplementEqualsCorrectly() {
    // Arrange
    var log1 = new FeatureUsageLog("tenant-123", "feature1", "/api/v1/test");
    log1.setId(1L);

    var log2 = new FeatureUsageLog("tenant-123", "feature1", "/api/v1/test");
    log2.setId(1L);

    var log3 = new FeatureUsageLog("tenant-123", "feature1", "/api/v1/test");
    log3.setId(2L);

    // Act & Assert
    assertThat(log1).isEqualTo(log2);
    assertThat(log1).isNotEqualTo(log3);
    assertThat(log1).isEqualTo(log1);
    assertThat(log1).isNotEqualTo(null);
    assertThat(log1).isNotEqualTo("not a log");
  }

  @Test
  @DisplayName("Should implement hashCode correctly")
  void shouldImplementHashCodeCorrectly() {
    // Arrange
    var log1 = new FeatureUsageLog("tenant-123", "feature1", "/api/v1/test");
    log1.setId(1L);

    var log2 = new FeatureUsageLog("tenant-123", "feature1", "/api/v1/test");
    log2.setId(1L);

    // Act & Assert
    assertThat(log1.hashCode()).isEqualTo(log2.hashCode());
  }

  @Test
  @DisplayName("Should implement toString correctly")
  void shouldImplementToStringCorrectly() {
    // Arrange
    var log = new FeatureUsageLog("tenant-123", "advanced_analytics", "/api/v1/analytics");
    log.setId(1L);
    var timestamp = Instant.now();
    log.setTimestamp(timestamp);

    // Act
    String result = log.toString();

    // Assert
    assertThat(result).contains("FeatureUsageLog");
    assertThat(result).contains("id=1");
    assertThat(result).contains("tenantId='tenant-123'");
    assertThat(result).contains("featureKey='advanced_analytics'");
    assertThat(result).contains("endpoint='/api/v1/analytics'");
  }

  @Test
  @DisplayName("Should handle null metadata")
  void shouldHandleNullMetadata() {
    // Arrange
    var log = new FeatureUsageLog("tenant-123", "feature1", "/api/v1/test");

    // Act
    log.setMetadata(null);

    // Assert
    assertThat(log.getMetadata()).isNull();
  }

  @Test
  @DisplayName("Should handle empty metadata")
  void shouldHandleEmptyMetadata() {
    // Arrange
    var log = new FeatureUsageLog("tenant-123", "feature1", "/api/v1/test");
    var emptyMetadata = Map.<String, Object>of();

    // Act
    log.setMetadata(emptyMetadata);

    // Assert
    assertThat(log.getMetadata()).isEmpty();
  }
}
