package com.iqscaffold.billingservice.subscription.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import com.iqscaffold.billingservice.subscription.InvoiceStatus;
import com.iqscaffold.billingservice.subscription.SubscriptionInvoice;
import com.iqscaffold.billingservice.webhook.WebhookEvent;
import com.stripe.model.Invoice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StripeInvoiceDataExtractorTest {

  private StripeInvoiceDataExtractor extractor;

  @BeforeEach
  void setUp() {
    extractor = new StripeInvoiceDataExtractor();
  }

  @Test
  @DisplayName("Should extract basic invoice data from webhook event")
  void shouldExtractBasicInvoiceData() {
    // Arrange
    var event = mock(WebhookEvent.class);
    when(event.resourceId()).thenReturn("inv_123");
    when(event.getMetadataString("subscription")).thenReturn(Optional.of("sub_456"));

    // Act
    var invoiceData = extractor.extractBasicData(event);

    // Assert
    assertThat(invoiceData.invoiceId()).isEqualTo("inv_123");
    assertThat(invoiceData.subscriptionId()).isEqualTo("sub_456");
  }

  @Test
  @DisplayName("Should handle missing subscription in metadata")
  void shouldHandleMissingSubscriptionInMetadata() {
    // Arrange
    var event = mock(WebhookEvent.class);
    when(event.resourceId()).thenReturn("inv_123");
    when(event.getMetadataString("subscription")).thenReturn(Optional.empty());

    // Act
    var invoiceData = extractor.extractBasicData(event);

    // Assert
    assertThat(invoiceData.invoiceId()).isEqualTo("inv_123");
    assertThat(invoiceData.subscriptionId()).isNull();
  }

  @Test
  @DisplayName("Should extract Stripe invoice from webhook event")
  void shouldExtractStripeInvoiceFromWebhookEvent() {
    // Arrange
    var event = mock(WebhookEvent.class);
    var stripeInvoice = mock(Invoice.class);
    when(event.metadata()).thenReturn(Map.of("invoice", stripeInvoice));

    // Act
    var result = extractor.extractStripeInvoice(event);

    // Assert
    assertThat(result).isEqualTo(stripeInvoice);
  }

  @Test
  @DisplayName("Should return null when invoice not in metadata")
  void shouldReturnNullWhenInvoiceNotInMetadata() {
    // Arrange
    var event = mock(WebhookEvent.class);
    when(event.metadata()).thenReturn(Map.of());

    // Act
    var result = extractor.extractStripeInvoice(event);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should return null when invoice is wrong type")
  void shouldReturnNullWhenInvoiceIsWrongType() {
    // Arrange
    var event = mock(WebhookEvent.class);
    when(event.metadata()).thenReturn(Map.of("invoice", "not an invoice"));

    // Act
    var result = extractor.extractStripeInvoice(event);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("Should populate invoice from Stripe invoice")
  void shouldPopulateInvoiceFromStripeInvoice() {
    // Arrange
    var invoice = new SubscriptionInvoice();
    var stripeInvoice = mock(Invoice.class);

    when(stripeInvoice.getId()).thenReturn("inv_123");
    when(stripeInvoice.getNumber()).thenReturn("INV-2024-001");
    when(stripeInvoice.getAmountDue()).thenReturn(2999L);
    when(stripeInvoice.getAmountPaid()).thenReturn(2999L);
    when(stripeInvoice.getCurrency()).thenReturn("usd");
    when(stripeInvoice.getStatus()).thenReturn("paid");
    when(stripeInvoice.getHostedInvoiceUrl()).thenReturn("https://invoice.stripe.com/i/123");
    when(stripeInvoice.getInvoicePdf()).thenReturn("https://invoice.stripe.com/i/123/pdf");
    when(stripeInvoice.getDueDate()).thenReturn(1704067200L);

    // Act
    extractor.populateFromStripeInvoice(invoice, stripeInvoice);

    // Assert
    assertThat(invoice.getStripeInvoiceId()).isEqualTo("inv_123");
    assertThat(invoice.getInvoiceNumber()).isEqualTo("INV-2024-001");
    assertThat(invoice.getAmountDue()).isEqualByComparingTo(new BigDecimal("29.99"));
    assertThat(invoice.getAmountPaid()).isEqualByComparingTo(new BigDecimal("29.99"));
    assertThat(invoice.getCurrency()).isEqualTo("USD");
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    assertThat(invoice.getHostedInvoiceUrl()).isEqualTo("https://invoice.stripe.com/i/123");
    assertThat(invoice.getInvoicePdfUrl()).isEqualTo("https://invoice.stripe.com/i/123/pdf");
    assertThat(invoice.getDueDate()).isNotNull();
  }

  @Test
  @DisplayName("Should handle null amounts in Stripe invoice")
  void shouldHandleNullAmountsInStripeInvoice() {
    // Arrange
    var invoice = new SubscriptionInvoice();
    var stripeInvoice = mock(Invoice.class);

    when(stripeInvoice.getId()).thenReturn("inv_123");
    when(stripeInvoice.getNumber()).thenReturn("INV-2024-001");
    when(stripeInvoice.getAmountDue()).thenReturn(null);
    when(stripeInvoice.getAmountPaid()).thenReturn(null);
    when(stripeInvoice.getCurrency()).thenReturn("usd");
    when(stripeInvoice.getStatus()).thenReturn("draft");
    when(stripeInvoice.getDueDate()).thenReturn(null);

    // Act
    extractor.populateFromStripeInvoice(invoice, stripeInvoice);

    // Assert
    // When amounts are null, they are not set, so they remain at default value
    assertThat(invoice.getDueDate()).isNull();
  }

  @Test
  @DisplayName("Should map Stripe status to local status - paid")
  void shouldMapStripeStatusToLocalStatusPaid() {
    // Act
    var status = extractor.mapStripeStatusToLocal("paid");

    // Assert
    assertThat(status).isEqualTo(InvoiceStatus.PAID);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - draft")
  void shouldMapStripeStatusToLocalStatusDraft() {
    // Act
    var status = extractor.mapStripeStatusToLocal("draft");

    // Assert
    assertThat(status).isEqualTo(InvoiceStatus.DRAFT);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - open")
  void shouldMapStripeStatusToLocalStatusOpen() {
    // Act
    var status = extractor.mapStripeStatusToLocal("open");

    // Assert
    assertThat(status).isEqualTo(InvoiceStatus.OPEN);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - void")
  void shouldMapStripeStatusToLocalStatusVoid() {
    // Act
    var status = extractor.mapStripeStatusToLocal("void");

    // Assert
    assertThat(status).isEqualTo(InvoiceStatus.VOID);
  }

  @Test
  @DisplayName("Should map Stripe status to local status - uncollectible")
  void shouldMapStripeStatusToLocalStatusUncollectible() {
    // Act
    var status = extractor.mapStripeStatusToLocal("uncollectible");

    // Assert
    assertThat(status).isEqualTo(InvoiceStatus.UNCOLLECTIBLE);
  }

  @Test
  @DisplayName("Should default to OPEN for unknown Stripe status")
  void shouldDefaultToOpenForUnknownStripeStatus() {
    // Act
    var status = extractor.mapStripeStatusToLocal("unknown_status");

    // Assert
    assertThat(status).isEqualTo(InvoiceStatus.OPEN);
  }

  @Test
  @DisplayName("Should handle case insensitive status mapping")
  void shouldHandleCaseInsensitiveStatusMapping() {
    // Act
    var status1 = extractor.mapStripeStatusToLocal("PAID");
    var status2 = extractor.mapStripeStatusToLocal("Paid");
    var status3 = extractor.mapStripeStatusToLocal("PaId");

    // Assert
    assertThat(status1).isEqualTo(InvoiceStatus.PAID);
    assertThat(status2).isEqualTo(InvoiceStatus.PAID);
    assertThat(status3).isEqualTo(InvoiceStatus.PAID);
  }
}
