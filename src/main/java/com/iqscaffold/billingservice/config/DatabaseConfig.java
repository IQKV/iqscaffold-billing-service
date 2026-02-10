package com.iqscaffold.billingservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for Billing Service. Configures JPA repositories, entity scanning, and transaction management.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.iqscaffold.billingservice.admin",
        "com.iqscaffold.billingservice.feature",
        "com.iqscaffold.billingservice.payment",
        "com.iqscaffold.billingservice.payout",
        "com.iqscaffold.billingservice.subscription",
        "com.iqscaffold.billingservice.webhook"
    },
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
@EntityScan(basePackages = {
    "com.iqscaffold.billingservice.admin",
    "com.iqscaffold.billingservice.feature",
    "com.iqscaffold.billingservice.payment",
    "com.iqscaffold.billingservice.payout",
    "com.iqscaffold.billingservice.subscription",
    "com.iqscaffold.billingservice.webhook"
})
@EnableTransactionManagement
public class DatabaseConfig {
  // Entities and repositories are organized by domain modules
}
