package com.iqscaffold.billingservice.subscription;

/**
 * Billing interval for subscription plans.
 */
public enum SubscriptionInterval {
  /**
   * Daily billing interval.
   */
  DAY,

  /**
   * Weekly billing interval.
   */
  WEEK,

  /**
   * Monthly billing interval.
   */
  MONTH,

  /**
   * Monthly billing interval (alias for backward compatibility).
   */
  MONTHLY,

  /**
   * Quarterly billing interval (3 months).
   */
  QUARTER,

  /**
   * Yearly billing interval.
   */
  YEAR
}
