# Project Name 💰

<!-- TEMPLATE: This README.template.md is a starter template. Copy parts into your real README.md and replace placeholders. -->

<details>
  <summary><strong>How to use this template (click to expand)</strong></summary>

1. Rename the title above to your service name and optionally add a logo right below it.
2. Add badges (build, license) under the title.
3. Fill each section below with your actual service content.
4. Update the API tables to reflect your actual endpoints and authority requirements.
5. Update the payment state machine table to match your actual state transitions.
6. Update the bounded contexts list to match your actual packages.
7. Update the environment variables table to match your `BillingProperties` bindings.
8. Remove this guidance block after you finish customizing.

</details>

- Add your service logo.
- Write a short introduction — what the service owns and which platform it belongs to.
- If you are using badges, add them here.

<details>
  <summary><strong>Badge examples (optional)</strong></summary>

- Build: <code>![CI](https://img.shields.io/github/actions/workflow/status/ORG/REPO/build-java-project.yml?label=CI)</code>
- License: <code>![License](https://img.shields.io/github/license/ORG/REPO)</code>
- Java: <code>![Java](https://img.shields.io/badge/java-21-blue)</code>
- Spring Boot: <code>![Spring Boot](https://img.shields.io/badge/spring--boot-3.x-brightgreen)</code>

</details>

## About

Describe the service's responsibilities in plain language:

- What financial domain does it own (payments, subscriptions, invoices, payouts, merchant onboarding)?
- Which payment gateways does it integrate with?
- What does it produce for other services (feature entitlements, billing events)?

## Quick Links

- [API Documentation](./docs/api/README.md)
- [Architecture Overview](./docs/architecture/README.md)
- [Deployment Guide](./docs/deployment/README.md)
- [Contributing Guidelines](.github/CONTRIBUTING.md)

## API

Base path: `/api/v1`

### Payment Operations

| Method | Path                            | Auth                 | Description               |
| ------ | ------------------------------- | -------------------- | ------------------------- |
| `POST` | `/billing/payments/intent`      | JWT                  | Create a payment intent   |
| `GET`  | `/billing/payments/{id}`        | JWT                  | Get payment by ID         |
| `GET`  | `/billing/payments`             | JWT `BILLING_ACCESS` | List payments (paginated) |
| `POST` | `/billing/payments/{id}/refund` | JWT `BILLING_ADMIN`  | Process full refund       |

### Subscription Operations

| Method | Path                                 | Auth                | Description             |
| ------ | ------------------------------------ | ------------------- | ----------------------- |
| `POST` | `/billing/subscriptions`             | JWT `BILLING_ADMIN` | Create subscription     |
| `GET`  | `/billing/subscriptions/active`      | JWT                 | Get active subscription |
| `GET`  | `/billing/subscriptions/{id}`        | JWT                 | Get subscription by ID  |
| `PUT`  | `/billing/subscriptions/{id}`        | JWT `BILLING_ADMIN` | Update subscription     |
| `POST` | `/billing/subscriptions/{id}/cancel` | JWT `BILLING_ADMIN` | Cancel at period end    |
| `POST` | `/billing/subscriptions/{id}/pause`  | JWT `BILLING_ADMIN` | Pause billing           |
| `POST` | `/billing/subscriptions/{id}/resume` | JWT `BILLING_ADMIN` | Resume billing          |

### Subscription Plan Management

| Method | Path                                    | Auth                | Description            |
| ------ | --------------------------------------- | ------------------- | ---------------------- |
| `POST` | `/billing/subscription-plans`           | JWT `BILLING_ADMIN` | Create plan            |
| `GET`  | `/billing/subscription-plans`           | public              | List plans (paginated) |
| `GET`  | `/billing/subscription-plans/active`    | public              | List active plans      |
| `PUT`  | `/billing/subscription-plans/{id}`      | JWT `BILLING_ADMIN` | Update plan            |
| `POST` | `/billing/subscription-plans/{id}/sync` | JWT `BILLING_ADMIN` | Sync with Stripe       |

### Invoice & Payout Operations

| Method | Path                     | Auth                 | Description        |
| ------ | ------------------------ | -------------------- | ------------------ |
| `GET`  | `/billing/invoices`      | JWT `BILLING_ACCESS` | List invoices      |
| `GET`  | `/billing/invoices/{id}` | JWT                  | Get invoice by ID  |
| `GET`  | `/billing/invoices/open` | JWT `BILLING_ACCESS` | List open invoices |
| `GET`  | `/billing/payouts`       | JWT `BILLING_ACCESS` | List payouts       |
| `GET`  | `/billing/payouts/{id}`  | JWT                  | Get payout by ID   |

### Admin & Gateway Configuration

| Method | Path                                                   | Auth                 | Description                        |
| ------ | ------------------------------------------------------ | -------------------- | ---------------------------------- |
| `POST` | `/admin/billing/merchants/onboard`                     | JWT `BILLING_ADMIN`  | Initiate Stripe Connect onboarding |
| `GET`  | `/admin/billing/merchants/status/{orgId}`              | JWT `BILLING_ACCESS` | Check merchant status              |
| `POST` | `/admin/billing/gateway-config`                        | JWT `BILLING_ADMIN`  | Create gateway config              |
| `PUT`  | `/admin/billing/gateway-config/{provider}`             | JWT `BILLING_ADMIN`  | Update gateway config              |
| `GET`  | `/admin/billing/gateway-config`                        | JWT `BILLING_ACCESS` | List gateway configs (masked)      |
| `POST` | `/admin/billing/gateway-config/{provider}/set-primary` | JWT `BILLING_ADMIN`  | Set primary gateway                |

### Webhooks & Internal

| Method | Path                            | Auth      | Description                      |
| ------ | ------------------------------- | --------- | -------------------------------- |
| `POST` | `/billing/webhooks/{provider}`  | Signature | Unified webhook endpoint         |
| `GET`  | `/internal/features/{tenantId}` | Internal  | Feature entitlements for gateway |

> Replace with your actual endpoints. Document the authority requirement for each route.

## Payment State Machine

The service enforces strict state transitions for financial integrity:

| From         | Event        | To                   | Notes                      |
| ------------ | ------------ | -------------------- | -------------------------- |
| `null`       | Create       | `PENDING`            | Initial record creation    |
| `PENDING`    | API call     | `PROCESSING`         | Intent sent to gateway     |
| `PROCESSING` | Webhook      | `SUCCEEDED`          | Success confirmation       |
| `PROCESSING` | Webhook      | `FAILED`             | Payment declined           |
| `SUCCEEDED`  | Admin action | `REFUNDED`           | Full refund                |
| `SUCCEEDED`  | Webhook      | `PARTIALLY_REFUNDED` | Partial refund             |
| `*`          | Webhook      | `CANCELED`           | Intent expired or canceled |

> Update to match your actual `PaymentStateMachine` transitions.

## Subscription Status Values

| Status     | Description                        |
| ---------- | ---------------------------------- |
| `active`   | Billing normally                   |
| `trialing` | In trial period, no charges yet    |
| `past_due` | Payment failed, in grace period    |
| `canceled` | Will end at period end             |
| `unpaid`   | Payment failed beyond grace period |
| `paused`   | Temporarily suspended by tenant    |

## Webhook Processing

The service uses a unified webhook architecture across all payment providers:

1. `POST /api/v1/billing/webhooks/{provider}` receives the raw event
2. The provider adapter (`StripePaymentProvider`, etc.) verifies the signature and normalizes the event into a `WebhookEvent`
3. `UnifiedWebhookService` routes to the appropriate handler:
    - `PaymentWebhookEventHandler` — payment lifecycle events
    - `PayoutWebhookEventHandler` — payout events
    - `AccountWebhookEventHandler` — merchant account capability changes
    - `SubscriptionWebhookEventHandler` — subscription lifecycle events
    - `InvoiceWebhookEventHandler` — invoice events

Webhook processing is idempotent — safe to receive the same event multiple times.

> Update handler names to match your actual implementations.

## Payment Gateway Configuration

Each tenant can configure their own gateway credentials, stored encrypted with AES-256-GCM:

- **Supported gateways**: Stripe (full), PayPal / Square / Braintree (future)
- **Credential encryption**: `GatewayConfigEncryptionService` uses AES-256-GCM with tenant-specific key derivation
- **Masked responses**: API responses show only the last 4 characters of sensitive fields
- **Primary gateway**: Tenants designate a default gateway; fallback to global config if none set

## Multi-Tenancy

Strategy: **schema-per-tenant** via Hibernate `MultiTenantConnectionProvider`.

**Public schema tables** (shared):

- `merchant_payment_config` — maps tenants to payment gateway accounts
- `payment_gateway_config` — encrypted tenant-specific gateway credentials

**Tenant schema tables** (isolated per tenant):

- `payment`, `payment_audit_trail` — payment records and state transition log
- `payout` — payout records from gateway
- `stripe_customer` — Stripe customer records
- `tenant_subscription`, `subscription_plan`, `subscription_invoice` — subscription data

`TenantContext` holds the current tenant in a thread-local; `SchemaTenantIdentifierResolver` routes Hibernate sessions to the correct schema.

## Tech Stack

- Java 21 / Spring Boot 3.x
- Spring Data JPA + Hibernate (schema-per-tenant) + PostgreSQL
- Liquibase for system and per-tenant schema migrations
- Redis for caching (Ehcache 3 as Hibernate L2 cache)
- RabbitMQ for billing domain events and notifications
- Stripe Java SDK (Connect, PaymentIntents, Subscriptions, Webhooks)
- Resilience4j (circuit breaker + time limiter for external gateway calls)
- Thymeleaf for transactional email templates
- Spring Modulith for module boundary enforcement
- Micrometer + Prometheus + OpenTelemetry tracing

## Prerequisites

- JDK 21 (Eclipse Temurin)
- Maven 3.9+
- Node.js >= 22.x & pnpm (git hooks)
- Docker & Docker Compose

## Quick Start

```bash
# Clone the repository
git clone https://github.com/ORG/REPO.git
cd REPO

# Install git hooks
pnpm install

# Copy environment variables
cp .env.example .env.local
# Edit .env.local — set STRIPE_API_KEY, STRIPE_WEBHOOK_SECRET, etc.

# Start dependencies (PostgreSQL, Redis, RabbitMQ)
docker compose up -d

# Run the service
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# → API:      http://localhost:8080
# → Actuator: http://localhost:8081/actuator/health
# → Swagger:  http://localhost:8080/swagger-ui.html
```

## Bounded Contexts (Spring Modulith)

```
src/main/java/com/iqscaffold/billingservice/
├── admin/              # Merchant onboarding, payment gateway config management
├── feature/            # Feature entitlements — FeatureEnablementService, usage tracking
├── payment/            # Payment intents, refunds, state machine, Stripe provider, audit trail
├── payout/             # Payout tracking from gateway to merchant bank accounts
├── security/           # JWT context, billing authorization, gateway credential encryption
├── subscription/       # Subscription lifecycle, plans, invoices, state machine, events
│   └── webhook/        # Subscription and invoice webhook event handlers
├── tenancy/            # Schema-per-tenant, TenantContext, Liquibase runner
├── webhook/            # Unified webhook service, provider-agnostic event handlers
├── shared/             # Email, notifications, messaging, JSON converters
└── infrastructure/     # Spring config, JPA, Redis, RabbitMQ, email setup
```

> Update module names to match your actual packages. Spring Modulith enforces module boundaries.

## Events Published to RabbitMQ

| Routing Key                     | Trigger                            |
| ------------------------------- | ---------------------------------- |
| `billing.payment.created`       | Payment intent created             |
| `billing.payment.succeeded`     | Payment confirmed via webhook      |
| `billing.payment.failed`        | Payment declined                   |
| `billing.payment.refunded`      | Refund processed                   |
| `billing.subscription.created`  | New subscription                   |
| `billing.subscription.canceled` | Subscription canceled              |
| `billing.subscription.paused`   | Subscription paused                |
| `billing.invoice.generated`     | Invoice created                    |
| `billing.invoice.paid`          | Invoice payment confirmed          |
| `billing.payout.paid`           | Payout sent to bank                |
| `billing.merchant.onboarded`    | Stripe Connect onboarding complete |
| `notification.email`            | Transactional email trigger        |

> Update routing keys to match your actual `MessagingService` configuration.

## Environment Variables

| Variable                                    | Default                                               | Description                                              |
| ------------------------------------------- | ----------------------------------------------------- | -------------------------------------------------------- |
| `IQSCAFFOLD_DATABASE_URL`                   | `jdbc:postgresql://localhost:5432/iqscaffold_billing` | PostgreSQL JDBC URL                                      |
| `IQSCAFFOLD_DATABASE_USERNAME`              | `iqscaffold_billing`                                  | Database user                                            |
| `IQSCAFFOLD_DATABASE_PASSWORD`              | `iqkv_password`                                       | Database password                                        |
| `IQSCAFFOLD_CACHE_REDIS_HOST`               | `localhost`                                           | Redis host                                               |
| `IQSCAFFOLD_CACHE_REDIS_PORT`               | `6379`                                                | Redis port                                               |
| `IQSCAFFOLD_CACHE_REDIS_DATABASE`           | `1`                                                   | Redis database index                                     |
| `IQSCAFFOLD_MESSAGING_RABBITMQ_HOST`        | `localhost`                                           | RabbitMQ host                                            |
| `IQSCAFFOLD_MESSAGING_RABBITMQ_USERNAME`    | `iqscaffold`                                          | RabbitMQ user                                            |
| `IQSCAFFOLD_MESSAGING_RABBITMQ_PASSWORD`    | `iqkv_password`                                       | RabbitMQ password                                        |
| `USER_SERVICE_URL`                          | `http://iqscaffold-user-service`                      | User service URL (for JWKS)                              |
| `STRIPE_API_KEY`                            | _(empty)_                                             | Stripe secret key                                        |
| `STRIPE_WEBHOOK_SECRET`                     | _(empty)_                                             | Stripe webhook signing secret                            |
| `STRIPE_CLIENT_ID`                          | _(empty)_                                             | Stripe Connect client ID                                 |
| `GATEWAY_CONFIG_ENCRYPTION_KEY`             | `change-this-...`                                     | AES-256-GCM master key for gateway credential encryption |
| `BILLING_SAAS_MODE`                         | `true`                                                | Enable platform fee (SaaS commission) on transactions    |
| `PAYMENT_PROVIDER`                          | `stripe`                                              | Default payment provider                                 |
| `SMTP_HOST`                                 | `localhost`                                           | SMTP host for transactional email                        |
| `EMAIL_FROM_EMAIL`                          | `billing@iqkv.dev`                                    | Sender address                                           |
| `APP_BASE_URL`                              | `https://app.iqkv.dev`                                | Frontend base URL (used in email links)                  |
| `IQSCAFFOLD_OBSERVABILITY_TRACING_ENDPOINT` | `http://localhost:4317`                               | OpenTelemetry OTLP endpoint                              |

> Copy `.env.example` to `.env.local` / `.env.uat` / `.env.prd` and fill in values.

## Maven Commands

```bash
# Build and test (skip Checkstyle during development)
./mvnw clean verify -Dcheckstyle.skip=true

# Run tests only
./mvnw test -Dcheckstyle.skip=true

# Explicit Checkstyle check
./mvnw checkstyle:check

# Coverage report → target/site/jacoco/index.html
./mvnw jacoco:report

# Production build
./mvnw clean package -Pproduction
```

## Docker

```bash
# Build image
docker build -t ORG/REPO:latest .

# Run full stack (service + all dependencies)
docker compose -f compose.container.yaml up -d
```

## Monitoring

| Endpoint                   | Description                 |
| -------------------------- | --------------------------- |
| `GET /actuator/health`     | Liveness + readiness probes |
| `GET /actuator/metrics`    | Application metrics         |
| `GET /actuator/prometheus` | Prometheus scrape endpoint  |
| `GET /swagger-ui.html`     | API documentation           |

A Grafana dashboard (`docs/monitoring/grafana-dashboard.json`) provides real-time visibility into service health, HTTP metrics, JVM memory, HikariCP pool usage, and billing-specific business metrics.

---

<details>
  <summary><strong>✅ Pre-publish checklist (remove in final README)</strong></summary>

- [ ] Title updated and logo added
- [ ] Badges added (CI, license)
- [ ] About section completed
- [ ] API tables reflect actual endpoints and authority requirements
- [ ] Payment state machine table matches actual `PaymentStateMachine` transitions
- [ ] Subscription status values match actual domain
- [ ] Webhook handler names match actual implementations
- [ ] Bounded contexts list matches actual Spring Modulith modules
- [ ] Events table matches actual `MessagingService` routing keys
- [ ] Environment variables table is complete
- [ ] Links verified (docs, external resources)
- [ ] Guidance blocks removed before publishing

</details>

---

## 🧩 Boilerplate Architecture

- **Persistence**: Spring Data JPA + Hibernate schema-per-tenant; Liquibase manages system and per-tenant schemas; Ehcache 3 as Hibernate L2 cache
- **Payment gateway**: Stripe Java SDK with Connect support; `PaymentProviderFactory` for runtime gateway selection; `PaymentStateMachine` enforces legal state transitions
- **Security**: Spring Security OAuth2 Resource Server (JWKS from user service); `BillingAuthorizationService` for fine-grained access control; AES-256-GCM encryption for gateway credentials
- **Webhooks**: Unified webhook architecture — provider adapters normalize events; idempotent handlers; cryptographic signature verification per provider
- **Resilience**: Resilience4j circuit breaker + time limiter on external Stripe API calls
- **Messaging**: RabbitMQ for billing domain events and notification triggers; dead-letter queue; publisher confirms
- **Email**: Thymeleaf HTML templates; Spring Mail; i18n support (en/es/fr); async delivery
- **Module boundaries**: Spring Modulith enforces inter-module dependencies
- **Testing**: JUnit 5 + Mockito; Testcontainers (PostgreSQL); H2 for unit tests; Spring AMQP test; ArchUnit; Spring Modulith test
- **Observability**: OpenTelemetry tracing (OTLP); Micrometer + Prometheus; structured JSON logging (Logstash encoder) with MDC; Grafana dashboard
- **GitHub Integration**: Issue templates, labels, Dependabot, and CI workflows
- **Quality Tools**: Checkstyle, JaCoCo (70% instruction gate), ArchUnit, oxfmt, commit convention enforcement

> See [AGENTS.md](AGENTS.md) for detailed project structure, DDD patterns, and AI agent guidelines.
