# Billing Service Database Migration Structure

This document explains the consolidated database migration structure for the iqscaffold-billing-service.

## Migration Organization

### System Migrations (`system/`)
- **Purpose**: System-wide billing tables and data in the `public` schema
- **Scope**: Cross-tenant entities like merchants, payment gateways, subscription plans, features
- **Execution**: Once per database instance
- **Naming**: Timestamp-based IDs (YYYYMMDDHHMM00-description)

### Tenant Migrations (`tenant/`)
- **Purpose**: Tenant-specific billing tables and data in tenant schemas
- **Scope**: Tenant subscriptions, invoices, feature usage tracking per tenant
- **Execution**: Once per tenant schema
- **Naming**: Timestamp-based IDs (YYYYMMDDHHMM00-description)

### Demo Data (`tenant/demo/`)
- **Purpose**: Sample billing data for development and testing
- **Context**: Only applied when `demo` context is active
- **Scope**: Test subscriptions, invoices, and usage data

## Migration Execution Order

### 1. System Migrations (Public Schema)
- **20251110160000**: Merchant configuration table (base structure)
- **20251110160001**: Payment gateway abstraction v1 (rename merchant tables)
- **20251110160100**: Initial payment schema (payments table)
- **20251110160200**: Customer and payout schema (gateway customers, payouts)
- **20251110160300**: Merchant capabilities (charges/payouts enabled flags)
- **20251110160400**: Merchant fees (application fee percentage)
- **20251110160500**: Payment gateway abstraction v2 (customer table abstraction)
- **20251110160600**: Organization integration (add organization_id to merchants)
- **20251110160700**: Payment gateway config table (tenant-specific gateway settings)
- **20251110160800**: Subscription schema (platform-wide subscription plans)
- **20251110160900**: Drop legacy features column (cleanup)
- **20251110160901**: Feature management schema (feature definitions, plan associations)

### 2. Tenant Migrations (Per-tenant Schema)
- **20251110161000**: Tenant subscription schema (subscriptions, invoices, items, audit)
- **20251110161100**: Feature usage tracking (usage logs, partitioning)

### 3. Demo Data (If demo context enabled)
- Sample billing records and configurations
- Test subscriptions and usage data

## Key Relationships

### System Schema (Public)
- `merchant_payment_config` ← `payment_gateway_config` [1:N] (tenant-specific gateway settings)
- `subscription_plan` ← `plan_feature` → `feature_definition` [M:N] (plan features)
- `payment_gateway_customer` (multi-gateway customer records)
- `payment` (payment transactions)
- `payout` (payout records)

### Tenant Schema (Per-tenant)
- `tenant_subscription` ← `subscription_invoice` [1:N]
- `tenant_subscription` ← `subscription_item` [1:N]
- `tenant_subscription` ← `tenant_subscription_audit_trail` [1:N]
- `feature_usage_log` (feature usage tracking)

## Consolidation Changes Made

1. ✅ **Moved root migrations**: Relocated all root changelog files to appropriate system folder
2. ✅ **Standardized naming**: All migrations now use timestamp-based naming (YYYYMMDDHHMM00)
3. ✅ **Fixed conflicts**: Resolved duplicate column definitions between merchants and capabilities
4. ✅ **Unified structure**: Created root master.xml for coordinated execution
5. ✅ **Updated change set IDs**: All change sets follow timestamp convention
6. ✅ **Proper ordering**: Migrations execute in logical dependency order

## Schema Architecture

### Multi-Gateway Support
- **merchant_payment_config**: Renamed from merchant_stripe_config for multi-gateway support
- **payment_gateway_customer**: Abstracted customer records supporting multiple providers
- **payment_gateway_config**: Tenant-specific gateway configurations
- **Gateway providers**: STRIPE, PAYPAL, SQUARE, BRAINTREE support

### Subscription Management
- **subscription_plan**: Platform-wide subscription plans (public schema)
- **tenant_subscription**: Tenant-specific subscription instances
- **subscription_invoice**: Billing invoices per tenant
- **subscription_item**: Line items for subscriptions

### Feature Management
- **feature_definition**: Platform-wide feature definitions
- **plan_feature**: Plan-feature associations with limits
- **feature_usage_log**: Per-tenant feature usage tracking

## Migration Files Structure

```
db/changelog/
├── master.xml                                           # Root orchestrator
├── system/
│   ├── master.xml                                      # System migrations orchestrator
│   ├── 20251110160000-merchants.xml
│   ├── 20251110160001-payment-gateway-abstraction-v1.xml
│   ├── 20251110160100-initial-payment-schema.xml
│   ├── 20251110160200-customer-payout-schema.xml
│   ├── 20251110160300-merchant-capabilities.xml
│   ├── 20251110160400-merchant-fees.xml
│   ├── 20251110160500-payment-gateway-abstraction.xml
│   ├── 20251110160600-add-organization-id-to-merchant-config.xml
│   ├── 20251110160700-payment-gateway-config-table.xml
│   ├── 20251110160800-subscription-schema.xml
│   ├── 20251110160900-drop-legacy-features-column.xml
│   ├── 20251110160901-feature-management-schema.xml
│   └── 20250121000000-demo-billing-records.xml
├── tenant/
│   ├── master.xml                                      # Tenant migrations orchestrator
│   ├── 20251110161000-tenant-subscription-schema.xml
│   ├── 20251110161100-feature-usage-tracking.xml
│   └── demo/
│       ├── master.xml                                  # Demo data orchestrator
│       ├── 20250121000001-demo-tenant-billing-data.xml
│       └── README.md
└── README.md                                           # This file
```

## Performance Considerations

- **Indexes**: Strategic indexes on tenant_id, gateway_provider, subscription status
- **Partitioning**: Feature usage logs support time-based partitioning
- **Foreign Keys**: Proper relationships between system and tenant schemas
- **Constraints**: Unique constraints on gateway accounts and tenant configurations