package com.iqscaffold.billingservice.subscription.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iqscaffold.billingservice.subscription.SubscriptionStatus;
import com.iqscaffold.billingservice.subscription.TenantSubscription;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import com.stripe.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StripeSubscriptionDataExtractorTest {

  private StripeSubscriptionDataExtractor extractor;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    extractor = new StripeSubscriptionDataExtractor(objectMapper);
  }

  @Test
  @DisplayName("Should extract basic subscription data from webhook event")
  void shouldExtractBasicSubscriptionData() {
    // Arrange
    var event = mock(WebhookEvent.class);
    when(event.resourceId()).thenReturn("sub_123");
    when(event.getMetadataString("status")).thenReturn(Optional.of("active"));
    when(event.getMetadataString("customer")).thenReturn(Optional.of("cus_456"));

    // Act
    var subscriptionData = extractor.extractBasicData(event);

    // Assert
    assertThat(subscriptionData.subscriptionId()).isEqualTo("sub_123");
    assertThat(subscriptionData.customerId()).isEqualTo("cus_456");
    assertThat(subscriptionData.status()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should default to active status when not provided")
  void shouldDefaultToActiveStatusWhenNotProvided() {
    // Arrange
    var event = mock(WebhookEvent.class);
    when(event.resourceId()).thenReturn("sub_123");
    when(event.getMetadataString("status")).thenReturn(Optional.empty());
    when(event.getMetadataString("customer")).thenReturn(Optional.of("cus_456"));

    // Act
    var subscriptionData = extractor.extractBasicData(event);

    // Assert
    assertThat(subscriptionData.status()).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should handle missing customer in metadata")
  void shouldHandleMissingCustomerInMetadata() {
    // Arrange
    var event = mock(WebhookEvent.class);
    when(event.resourceId()).thenReturn("sub_123");
    when(event.getMetadataString("status")).thenReturn(Optional.of("active"));
    when(event.getMetadataString("customer")).thenReturn(Optional.empty());

    // Act
    var subscriptionData = extractor.extractBasicData(event);

    // Assert
    assertThat(subscriptionData.customerId()).isNull();
  }

  @Test
  @DisplayName("Should map Stripe status to local status - active")
  void shouldMapStripeStatusToLocalStatusActive() {
    // Act
    var status = extractor.mapStripeStatusToLocal("active");

    // Assert
    assertThat(status).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - trialing")
  void shouldMapStripeStatusToLocalStatusTrialing() {
    // Act
    var status = extractor.mapStripeStatusToLocal("trialing");

    // Assert
    assertThat(status).isEqualTo(SubscriptionStatus.TRIALING);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - incomplete")
  void shouldMapStripeStatusToLocalStatusIncomplete() {
    // Act
    var status = extractor.mapStripeStatusToLocal("incomplete");

    // Assert
    assertThat(status).isEqualTo(SubscriptionStatus.INCOMPLETE);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - past_due")
  void shouldMapStripeStatusToLocalStatusPastDue() {
    // Act
    var status = extractor.mapStripeStatusToLocal("past_due");

    // Assert
    assertThat(status).isEqualTo(SubscriptionStatus.PAST_DUE);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - canceled")
  void shouldMapStripeStatusToLocalStatusCanceled() {
    // Act
    var status = extractor.mapStripeStatusToLocal("canceled");

    // Assert
    assertThat(status).isEqualTo(SubscriptionStatus.CANCELED);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - unpaid")
  void shouldMapStripeStatusToLocalStatusUnpaid() {
    // Act
    var status = extractor.mapStripeStatusToLocal("unpaid");

    // Assert
    assertThat(status).isEqualTo(SubscriptionStatus.UNPAID);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - paused")
  void shouldMapStripeStatusToLocalStatusPaused() {
    // Act
    var status = extractor.mapStripeStatusToLocal("paused");

    // Assert
    assertThat(status).isEqualTo(SubscriptionStatus.PAUSED);
  }

  @Test
  @DisplayName("Should default to ACTIVE for unknown Stripe status")
  void shouldDefaultToActiveForUnknownStripeStatus() {
    // Act
    var status = extractor.mapStripeStatusToLocal("unknown_status");

    // Assert
    assertThat(status).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should handle case insensitive status mapping")
  void shouldHandleCaseInsensitiveStatusMapping() {
    // Act
    var status1 = extractor.mapStripeStatusToLocal("ACTIVE");
    var status2 = extractor.mapStripeStatusToLocal("Active");
    var status3 = extractor.mapStripeStatusToLocal("AcTiVe");

    // Assert
    assertThat(status1).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(status2).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(status3).isEqualTo(SubscriptionStatus.ACTIVE);
  }

  @Test
  @DisplayName("Should populate subscription from Stripe subscription object")
  void shouldPopulateSubscriptionFromStripeSubscriptionObject() {
    // Arrange
    var subscription = new TenantSubscription();
    var event = mock(WebhookEvent.class);
    var stripeSubscription = mock(Subscription.class);

    String jsonString = """
        {
          "current_period_start": 1704067200,
          "current_period_end": 1706745600,
          "trial_end": null,
          "cancel_at": null,
          "canceled_at": null
        }
        """;

    when(event.metadata()).thenReturn(Map.of("subscription", stripeSubscription));
    when(stripeSubscription.toJson()).thenReturn(jsonString);
    when(stripeSubscription.getTrialEnd()).thenReturn(null);
    when(stripeSubscription.getCancelAt()).thenReturn(null);
    when(stripeSubscription.getCanceledAt()).thenReturn(null);
    when(stripeSubscription.getCancellationDetails()).thenReturn(null);

    // Act
    extractor.populateFromStripeSubscription(subscription, event);

    // Assert
    assertThat(subscription.getCurrentPeriodStart()).isNotNull();
    assertThat(subscription.getCurrentPeriodEnd()).isNotNull();
  }

  @Test
  @DisplayName("Should handle Stripe subscription with trial end")
  void shouldHandleStripeSubscriptionWithTrialEnd() {
    // Arrange
    var subscription = new TenantSubscription();
    var event = mock(WebhookEvent.class);
    var stripeSubscription = mock(Subscription.class);

    String jsonString = """
        {
          "current_period_start": 1704067200,
          "current_period_end": 1706745600
        }
        """;

    when(event.metadata()).thenReturn(Map.of("subscription", stripeSubscription));
    when(stripeSubscription.toJson()).thenReturn(jsonString);
    when(stripeSubscription.getTrialEnd()).thenReturn(1705276800L);
    when(stripeSubscription.getCancelAt()).thenReturn(null);
    when(stripeSubscription.getCanceledAt()).thenReturn(null);
    when(stripeSubscription.getCancellationDetails()).thenReturn(null);

    // Act
    extractor.populateFromStripeSubscription(subscription, event);

    // Assert
    assertThat(subscription.getTrialEnd()).isNotNull();
  }

  @Test
  @DisplayName("Should handle Stripe subscription with cancellation")
  void shouldHandleStripeSubscriptionWithCancellation() {
    // Arrange
    var subscription = new TenantSubscription();
    var event = mock(WebhookEvent.class);
    var stripeSubscription = mock(Subscription.class);
    var cancellationDetails = mock(Subscription.CancellationDetails.class);

    String jsonString = """
        {
          "current_period_start": 1704067200,
          "current_period_end": 1706745600
        }
        """;

    when(event.metadata()).thenReturn(Map.of("subscription", stripeSubscription));
    when(stripeSubscription.toJson()).thenReturn(jsonString);
    when(stripeSubscription.getTrialEnd()).thenReturn(null);
    when(stripeSubscription.getCancelAt()).thenReturn(1706745600L);
    when(stripeSubscription.getCanceledAt()).thenReturn(1704067200L);
    when(stripeSubscription.getCancellationDetails()).thenReturn(cancellationDetails);
    when(cancellationDetails.getReason()).thenReturn("customer_request");

    // Act
    extractor.populateFromStripeSubscription(subscription, event);

    // Assert
    assertThat(subscription.getCancelAt()).isNotNull();
    assertThat(subscription.getCanceledAt()).isNotNull();
    assertThat(subscription.getCancellationReason()).isEqualTo("customer_request");
  }

  @Test
  @DisplayName("Should handle missing subscription in metadata")
  void shouldHandleMissingSubscriptionInMetadata() {
    // Arrange
    var subscription = new TenantSubscription();
    var event = mock(WebhookEvent.class);

    when(event.metadata()).thenReturn(Map.of());

    // Act & Assert - should not throw
    extractor.populateFromStripeSubscription(subscription, event);
  }

  @Test
  @DisplayName("Should handle wrong type in subscription metadata")
  void shouldHandleWrongTypeInSubscriptionMetadata() {
    // Arrange
    var subscription = new TenantSubscription();
    var event = mock(WebhookEvent.class);

    when(event.metadata()).thenReturn(Map.of("subscription", "not a subscription"));

    // Act & Assert - should not throw
    extractor.populateFromStripeSubscription(subscription, event);
  }
}
