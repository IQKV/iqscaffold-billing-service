## 📜 Deployment Guide

### Overview

The IQ Scaffold Billing Service is deployed using Helm charts and automated CI/CD pipelines. The service provides Stripe payment integration, subscription management, multi-tenancy, and webhook processing capabilities.

### Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- External infrastructure services (PostgreSQL, Redis, RabbitMQ)
- Stripe account for payment processing

### Environments

| Environment | Namespace                   | Purpose                      |
| ----------- | --------------------------- | ---------------------------- |
| Dev         | `iqscaffold-dev-env`        | Development and WIP branches |
| Test        | `iqscaffold-test-env`       | Feature branch testing       |
| Staging     | `iqscaffold-staging-env`    | Pre-production validation    |
| Production  | `iqscaffold-production-env` | Live production environment  |

### Automated Deployment (CI/CD)

#### Drone Pipeline Overview

The service uses a comprehensive Drone CI/CD pipeline with 10 stages:

1. **VerifyCode** - Code quality, tests, static analysis
2. **PublishArtifacts** - Maven artifacts to Nexus
3. **PublishDockerImage** - Container images to registry
4. **DeployWorkInProgressOnDev** - WIP branch auto-deployment
5. **RollbackWorkInProgressOnDev** - WIP rollback
6. **PromoteFeatureDeployment** - Feature branch promotion
7. **RollbackFeatureDeployment** - Feature rollback
8. **PromoteDeployment** - Release promotion
9. **RollbackDeployment** - Release rollback
10. **ReleasePackage** - Automated version management

#### Branch Deployment Strategy

| Branch Type | Auto Deploy | Manual Promote | Target Environment |
| ----------- | ----------- | -------------- | ------------------ |
| `wip`       | ✅ Dev      | -              | Dev                |
| `feature/*` | -           | ✅ Test        | Test               |
| `dev`       | -           | ✅ Staging     | Staging            |
| Tags        | -           | ✅ Production  | Production         |

#### Deployment Commands

The pipeline uses these Helm commands for deployment:

```bash
# Development (WIP branches)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-billing-service ./ \
  --values ./values.yaml \
  --values ./values-dev.yaml \
  --set image.tag=wip \
  --set infraServices.postgresql.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set infraServices.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --set config.billing.stripe.secretKey=${STRIPE_SECRET_KEY} \
  --set config.billing.stripe.webhookSecret=${STRIPE_WEBHOOK_SECRET} \
  --set config.encryption.masterKey=${ENCRYPTION_MASTER_KEY} \
  --namespace iqscaffold-dev-env

# Production (Tagged releases)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-billing-service ./ \
  --values ./values.yaml \
  --values ./values-production.yaml \
  --set image.tag=${DRONE_TAG} \
  --set infraServices.postgresql.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set infraServices.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --set config.billing.stripe.publicKey=${STRIPE_PUBLIC_KEY} \
  --set config.billing.stripe.secretKey=${STRIPE_SECRET_KEY} \
  --set config.billing.stripe.webhookSecret=${STRIPE_WEBHOOK_SECRET} \
  --set config.billing.stripe.connectClientId=${STRIPE_CONNECT_CLIENT_ID} \
  --set config.encryption.masterKey=${ENCRYPTION_MASTER_KEY} \
  --set config.email.smtp.password=${SMTP_PASSWORD} \
  --namespace iqscaffold-production-env
```

### Manual Deployment

#### Quick Start

```bash
# Clone Helm charts
git clone <HELM_CHARTS_REPOSITORY> charts
cd charts/IQKV/iqscaffold-billing-service

# Deploy to development
helm upgrade --install billing-service ./ \
  --values values-dev.yaml \
  --set infraServices.postgresql.password="your-db-password" \
  --set config.billing.stripe.secretKey="sk_test_your_stripe_key" \
  --set config.encryption.masterKey="your-32-char-encryption-key" \
  --namespace iqscaffold-dev-env \
  --create-namespace
```

#### Environment-Specific Deployments

#### Development

```bash
helm upgrade --install billing-service ./ \
  --values values-dev.yaml \
  --namespace iqscaffold-dev-env \
  --create-namespace
```

#### Production

```bash
helm upgrade --install billing-service ./ \
  --values values-production.yaml \
  --set infraServices.postgresql.password="${DB_PASSWORD}" \
  --set infraServices.redis.password="${REDIS_PASSWORD}" \
  --set infraServices.rabbitmq.password="${RABBITMQ_PASSWORD}" \
  --set config.billing.stripe.publicKey="${STRIPE_PUBLIC_KEY}" \
  --set config.billing.stripe.secretKey="${STRIPE_SECRET_KEY}" \
  --set config.billing.stripe.webhookSecret="${STRIPE_WEBHOOK_SECRET}" \
  --set config.billing.stripe.connectClientId="${STRIPE_CONNECT_CLIENT_ID}" \
  --set config.encryption.masterKey="${ENCRYPTION_MASTER_KEY}" \
  --set config.email.smtp.password="${SMTP_PASSWORD}" \
  --namespace iqscaffold-production-env \
  --create-namespace
```

### Configuration

#### Required Secrets

| Secret                | Environment Variable       | Required | Description                     |
| --------------------- | -------------------------- | -------- | ------------------------------- |
| Database Password     | `INFRA_POSTGRESQL_PASSWORD`  | ✅       | PostgreSQL password             |
| Stripe Secret Key     | `STRIPE_SECRET_KEY`        | ✅       | Stripe API secret key           |
| Stripe Webhook Secret | `STRIPE_WEBHOOK_SECRET`    | ✅       | Stripe webhook endpoint secret  |
| Encryption Master Key | `ENCRYPTION_MASTER_KEY`    | ✅       | Data encryption key (32+ chars) |
| RabbitMQ Password     | `INFRA_RABBITMQ_PASSWORD` | ⚠️       | Message broker password         |
| Redis Password        | `REDIS_PASSWORD`           | ⚠️       | Cache password                  |
| Stripe Public Key     | `STRIPE_PUBLIC_KEY`        | ⚠️       | Stripe publishable key          |
| Stripe Connect Client | `STRIPE_CONNECT_CLIENT_ID` | ⚠️       | Stripe Connect application ID   |
| SMTP Password         | `SMTP_PASSWORD`            | ⚠️       | Email service password          |

#### External Services

The service connects to these external infrastructure components:

- **PostgreSQL**: Billing data, subscriptions, invoices
- **Redis**: Payment session caching and rate limiting
- **RabbitMQ**: Billing event messaging
- **User Service**: User authentication and tenant management
- **Stripe**: Payment processing and webhooks

#### Encryption Master Key

The `config.encryption.masterKey` is a critical security component that encrypts sensitive payment gateway configuration data at rest in the database.

**What it encrypts:**

- Stripe API keys (`sk_live_...`, `sk_test_...`)
- Webhook secrets (`whsec_...`)
- Connect client IDs (`ca_...`)
- Other payment provider credentials

**How it works:**

- Uses AES-256-GCM encryption with tenant-specific key derivation
- Each tenant's data is encrypted with a unique key derived from the master key + tenant ID
- Provides authenticated encryption with integrity verification
- Uses unique initialization vectors per encryption operation

**Security requirements:**

- Minimum 32 characters for cryptographic security
- Must be cryptographically secure random string
- Different keys for each environment (dev/staging/production)
- Never store in version control - pass via `--set` flags only

**Impact:**

- Without this key, the service cannot decrypt stored payment configurations
- Multi-tenant payment processing will fail
- Tenant-specific Stripe configurations become inaccessible

```bash
# Example secure key generation
openssl rand -base64 32
```

#### Service Configuration

| Setting        | Dev      | Production       |
| -------------- | -------- | ---------------- |
| Replicas       | 1        | 2                |
| CPU Request    | 250m     | 500m             |
| Memory Request | 256Mi    | 512Mi            |
| Autoscaling    | Disabled | 2-10 replicas    |
| Ingress        | Disabled | Enabled with TLS |
| Monitoring     | Disabled | Enabled          |

### Monitoring & Health Checks

#### Health Endpoints

- **Liveness**: `/actuator/health/liveness` (port 8081)
- **Readiness**: `/actuator/health/readiness` (port 8081)
- **Metrics**: `/actuator/prometheus` (port 8081)

#### Monitoring Stack

Production deployments include:

- Prometheus ServiceMonitor
- Alerting rules for service health and payment failures
- Grafana dashboards for billing metrics

### Troubleshooting

#### Common Issues

1. **Database Connection Failures**

   ```bash
   kubectl logs deployment/iqscaffold-billing-service -n iqscaffold-dev-env
   ```

2. **Stripe Webhook Validation Errors**

   ```bash
   kubectl logs deployment/iqscaffold-billing-service -n iqscaffold-dev-env | grep "webhook"
   ```

3. **Check Configuration**

   ```bash
   kubectl describe configmap iqscaffold-billing-service-config -n iqscaffold-dev-env
   ```

4. **Test Health Endpoints**
   ```bash
   kubectl port-forward deployment/iqscaffold-billing-service 8081:8081 -n iqscaffold-dev-env
   curl http://localhost:8081/actuator/health
   ```

#### Rollback

```bash
# Rollback to previous version
helm rollback iqscaffold-billing-service -n iqscaffold-production-env

# Or uninstall completely
helm uninstall iqscaffold-billing-service -n iqscaffold-production-env
```

### Security

- All sensitive values passed via `--set` flags
- TLS enabled in production
- Network policies restrict pod communication
- Non-root container execution
- Read-only root filesystem in production
- Stripe webhook signature validation
- Data encryption at rest using master key
