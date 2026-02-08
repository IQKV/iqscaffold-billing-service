package com.iqscaffold.billingservice.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class FeatureUsageTrackingServiceTest {

  @Mock
  private FeatureUsageLogRepository usageLogRepository;

  @InjectMocks
  private FeatureUsageTrackingService trackingService;

  private String tenantId;
  private String featureKey;
  private String endpoint;

  @BeforeEach
  void setUp() {
    tenantId = "tenant-123";
    featureKey = "advanced_analytics";
    endpoint = "/api/v1/analytics/reports";
    MDC.clear();
  }

  @Test
  @DisplayName("Should record feature usage")
  void shouldRecordFeatureUsage() {
    // Arrange
    ArgumentCaptor<FeatureUsageLog> captor = ArgumentCaptor.forClass(FeatureUsageLog.class);

    // Act
    trackingService.recordUsage(tenantId, featureKey, endpoint);

    // Assert
    verify(usageLogRepository).save(captor.capture());
    FeatureUsageLog savedLog = captor.getValue();
    assertThat(savedLog.getTenantId()).isEqualTo(tenantId);
    assertThat(savedLog.getFeatureKey()).isEqualTo(featureKey);
    assertThat(savedLog.getEndpoint()).isEqualTo(endpoint);
  }

  @Test
  @DisplayName("Should record feature usage with MDC context")
  void shouldRecordFeatureUsageWithMdcContext() {
    // Arrange
    MDC.put("userId", "user-456");
    MDC.put("correlationId", "corr-789");
    MDC.put("sessionId", "sess-012");
    ArgumentCaptor<FeatureUsageLog> captor = ArgumentCaptor.forClass(FeatureUsageLog.class);

    // Act
    trackingService.recordUsage(tenantId, featureKey, endpoint);

    // Assert
    verify(usageLogRepository).save(captor.capture());
    FeatureUsageLog savedLog = captor.getValue();
    assertThat(savedLog.getUserId()).isEqualTo("user-456");
    assertThat(savedLog.getCorrelationId()).isEqualTo("corr-789");
    assertThat(savedLog.getSessionId()).isEqualTo("sess-012");

    MDC.clear();
  }

  @Test
  @DisplayName("Should record feature usage with metadata")
  void shouldRecordFeatureUsageWithMetadata() {
    // Arrange
    var metadata = Map.<String, Object>of("reportType", "sales", "format", "pdf");
    ArgumentCaptor<FeatureUsageLog> captor = ArgumentCaptor.forClass(FeatureUsageLog.class);

    // Act
    trackingService.recordUsage(tenantId, featureKey, endpoint, metadata);

    // Assert
    verify(usageLogRepository).save(captor.capture());
    FeatureUsageLog savedLog = captor.getValue();
    assertThat(savedLog.getMetadata()).isEqualTo(metadata);
  }

  @Test
  @DisplayName("Should not throw exception when recording fails")
  void shouldNotThrowExceptionWhenRecordingFails() {
    // Arrange
    doThrow(new RuntimeException("Database error"))
        .when(usageLogRepository).save(any(FeatureUsageLog.class));

    // Act & Assert - should not throw
    trackingService.recordUsage(tenantId, featureKey, endpoint);
  }

  @Test
  @DisplayName("Should get usage count for feature in time range")
  void shouldGetUsageCountForFeatureInTimeRange() {
    // Arrange
    Instant startTime = Instant.now().minus(7, ChronoUnit.DAYS);
    Instant endTime = Instant.now();
    when(usageLogRepository.countByTenantAndFeatureInTimeRange(tenantId, featureKey, startTime, endTime))
        .thenReturn(42L);

    // Act
    long count = trackingService.getUsageCount(tenantId, featureKey, startTime, endTime);

    // Assert
    assertThat(count).isEqualTo(42L);
  }

  @Test
  @DisplayName("Should get usage statistics for tenant")
  void shouldGetUsageStatisticsForTenant() {
    // Arrange
    Instant startTime = Instant.now().minus(30, ChronoUnit.DAYS);
    Instant endTime = Instant.now();
    List<Object[]> mockResults = List.of(
        new Object[]{"feature1", 100L},
        new Object[]{"feature2", 50L}
    );
    when(usageLogRepository.getFeatureUsageStatsByTenant(tenantId, startTime, endTime))
        .thenReturn(mockResults);

    // Act
    var statistics = trackingService.getUsageStatistics(tenantId, startTime, endTime);

    // Assert
    assertThat(statistics).hasSize(2);
    assertThat(statistics).containsEntry("feature1", 100L);
    assertThat(statistics).containsEntry("feature2", 50L);
  }

  @Test
  @DisplayName("Should get daily usage count")
  void shouldGetDailyUsageCount() {
    // Arrange
    when(usageLogRepository.countByTenantAndFeatureInTimeRange(
        eq(tenantId), eq(featureKey), any(Instant.class), any(Instant.class)))
        .thenReturn(15L);

    // Act
    long count = trackingService.getDailyUsageCount(tenantId, featureKey);

    // Assert
    assertThat(count).isEqualTo(15L);
  }

  @Test
  @DisplayName("Should get monthly usage count")
  void shouldGetMonthlyUsageCount() {
    // Arrange
    when(usageLogRepository.countByTenantAndFeatureInTimeRange(
        eq(tenantId), eq(featureKey), any(Instant.class), any(Instant.class)))
        .thenReturn(500L);

    // Act
    long count = trackingService.getMonthlyUsageCount(tenantId, featureKey);

    // Assert
    assertThat(count).isEqualTo(500L);
  }

  @Test
  @DisplayName("Should check if tenant has recent usage")
  void shouldCheckIfTenantHasRecentUsage() {
    // Arrange
    when(usageLogRepository.countByTenantAndFeatureInTimeRange(
        eq(tenantId), eq(featureKey), any(Instant.class), any(Instant.class)))
        .thenReturn(5L);

    // Act
    boolean hasRecentUsage = trackingService.hasRecentUsage(tenantId, featureKey);

    // Assert
    assertThat(hasRecentUsage).isTrue();
  }

  @Test
  @DisplayName("Should return false when tenant has no recent usage")
  void shouldReturnFalseWhenTenantHasNoRecentUsage() {
    // Arrange
    when(usageLogRepository.countByTenantAndFeatureInTimeRange(
        eq(tenantId), eq(featureKey), any(Instant.class), any(Instant.class)))
        .thenReturn(0L);

    // Act
    boolean hasRecentUsage = trackingService.hasRecentUsage(tenantId, featureKey);

    // Assert
    assertThat(hasRecentUsage).isFalse();
  }

  @Test
  @DisplayName("Should get most used features")
  void shouldGetMostUsedFeatures() {
    // Arrange
    int limit = 5;
    List<Object[]> mockResults = List.of(
        new Object[]{"api_access", 1000L},
        new Object[]{"advanced_reports", 750L},
        new Object[]{"export_data", 500L}
    );
    when(usageLogRepository.getMostUsedFeatures(any(Instant.class), any(Instant.class), any(PageRequest.class)))
        .thenReturn(mockResults);

    // Act
    var mostUsed = trackingService.getMostUsedFeatures(limit);

    // Assert
    assertThat(mostUsed).hasSize(3);
    assertThat(mostUsed).containsEntry("api_access", 1000L);
    assertThat(mostUsed).containsEntry("advanced_reports", 750L);
    assertThat(mostUsed).containsEntry("export_data", 500L);
  }

  @Test
  @DisplayName("Should cleanup old usage logs")
  void shouldCleanupOldUsageLogs() {
    // Arrange
    int retentionDays = 90;

    // Act
    trackingService.cleanupOldUsageLogs(retentionDays);

    // Assert
    verify(usageLogRepository).deleteByTimestampBefore(any(Instant.class));
  }

  @Test
  @DisplayName("Should handle cleanup failure gracefully")
  void shouldHandleCleanupFailureGracefully() {
    // Arrange
    int retentionDays = 90;
    doThrow(new RuntimeException("Cleanup failed"))
        .when(usageLogRepository).deleteByTimestampBefore(any(Instant.class));

    // Act & Assert - should not throw
    trackingService.cleanupOldUsageLogs(retentionDays);
  }

  @Test
  @DisplayName("Should handle empty usage statistics")
  void shouldHandleEmptyUsageStatistics() {
    // Arrange
    Instant startTime = Instant.now().minus(30, ChronoUnit.DAYS);
    Instant endTime = Instant.now();
    when(usageLogRepository.getFeatureUsageStatsByTenant(tenantId, startTime, endTime))
        .thenReturn(List.of());

    // Act
    var statistics = trackingService.getUsageStatistics(tenantId, startTime, endTime);

    // Assert
    assertThat(statistics).isEmpty();
  }

  @Test
  @DisplayName("Should not fail when metadata recording fails")
  void shouldNotFailWhenMetadataRecordingFails() {
    // Arrange
    var metadata = Map.<String, Object>of("key", "value");
    doThrow(new RuntimeException("Save failed"))
        .when(usageLogRepository).save(any(FeatureUsageLog.class));

    // Act & Assert - should not throw
    trackingService.recordUsage(tenantId, featureKey, endpoint, metadata);
  }
}
