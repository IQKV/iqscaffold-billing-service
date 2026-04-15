## 📜 Deployment Guide

### Overview

The IQ Key Value Billing Service is deployed using Helm charts and automated CI/CD pipelines. The service provides Stripe payment integration, subscription management, multi-tenancy, and webhook processing capabilities.

### Prerequisites

- Kubernetes 1.19+
- Helm 3.2.0+
- External infrastructure services (PostgreSQL, Redis, RabbitMQ)
- Stripe account for payment processing

### Environments

| Environment | Namespace      | Purpose                     |
| ----------- | -------------- | --------------------------- |
| Test        | `iqkv-sit-env` | Feature branch testing      |
| Staging     | `iqkv-uat-env` | Pre-production validation   |
| Production  | `iqkv-prd-env` | Live production environment |

### Automated Deployment (CI/CD)

#### Drone Pipeline Overview

<details>
<summary>📋 Pipeline Stages</summary>

The service uses Drone CI/CD pipeline with 10 stages:

1. **VerifyCode** - Code quality, tests, static analysis
2. **PublishArtifacts** - Maven artifacts to Nexus
3. **PublishDockerImage** - Container images to registry
4. **DeployWorkInProgress** - WIP branch auto-deployment
5. **RollbackWorkInProgress** - WIP rollback
6. **PromoteFeatureDeployment** - Feature branch promotion
7. **RollbackFeatureDeployment** - Feature rollback
8. **PromoteDeployment** - Release promotion
9. **RollbackDeployment** - Release rollback
10. **ReleasePackage** - Automated version management

</details>

<details>
<summary>🔐 Required Drone Secrets</summary>

| Secret Name                       | Purpose                              | Used In                                    |
| --------------------------------- | ------------------------------------ | ------------------------------------------ |
| `NEXUS_DEPLOYER_USERNAME`         | Nexus repository authentication      | Artifact publishing, dependency resolution |
| `NEXUS_DEPLOYER_PASSWORD`         | Nexus repository authentication      | Artifact publishing, dependency resolution |
| `SONAR_HOST`                      | SonarQube server URL                 | Static code analysis                       |
| `SONAR_TOKEN`                     | SonarQube authentication token       | Static code analysis                       |
| `SLACK_WEBHOOK`                   | Slack notifications webhook URL      | Build status notifications                 |
| `GITHUB_API_ACCESS_TOKEN`         | GitHub API access for releases       | Release creation, changelog generation     |
| `SVC_CONTAINER_REGISTRY_USERNAME` | Container registry authentication    | Docker image publishing                    |
| `SVC_CONTAINER_REGISTRY_PASSWORD` | Container registry authentication    | Docker image publishing                    |
| `HELM_CHARTS_REPOSITORY`          | Helm charts repository URL           | Kubernetes deployments                     |
| `INFRA_POSTGRESQL_PASSWORD`       | PostgreSQL database password         | Application configuration                  |
| `INFRA_REDIS_PASSWORD`            | Redis cache password                 | Application configuration                  |
| `INFRA_RABBITMQ_PASSWORD`         | RabbitMQ message broker password     | Application configuration                  |
| `STRIPE_PUBLIC_KEY`               | Stripe publishable API key           | Payment processing configuration           |
| `STRIPE_SECRET_KEY`               | Stripe secret API key                | Payment processing configuration           |
| `STRIPE_WEBHOOK_SECRET`           | Stripe webhook endpoint secret       | Webhook signature validation               |
| `STRIPE_CONNECT_CLIENT_ID`        | Stripe Connect application ID        | Multi-party payment processing             |
| `ENCRYPTION_MASTER_KEY`           | Data encryption master key           | Sensitive data encryption at rest          |
| `SMTP_PASSWORD`                   | Email service password               | Email notifications                        |
| `JWT_SECRET_KEY`                  | JWT symmetric validation key (HS256) | Request authentication validation          |

</details>

#### Branch Deployment Strategy

| Branch Type | Auto Deploy | Manual Promote | Target Environment |
| ----------- | ----------- | -------------- | ------------------ |
| `wip`       | ✅ Dev      | -              | Dev                |
| `feature/*` | -           | ✅ Test        | Test               |
| `dev`       | -           | ✅ Staging     | Staging            |
| Tags        | -           | ✅ Production  | Production         |

#### Deployment Commands

The pipeline uses these Helm commands for deployment:

<details>
<summary>Helm Commands</summary>

```bash
# Development (WIP branches)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-billing-service ./ \
  --values ./values.yaml \
  --values ./values-test.yaml \
  --set image.tag=wip \
  --set infraServices.postgresql.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set infraServices.redis.password=${INFRA_REDIS_PASSWORD} \
  --set infraServices.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --set config.billing.stripe.publicKey=${STRIPE_PUBLIC_KEY} \
  --set config.billing.stripe.secretKey=${STRIPE_SECRET_KEY} \
  --set config.billing.stripe.webhookSecret=${STRIPE_WEBHOOK_SECRET} \
  --set config.billing.stripe.connectClientId=${STRIPE_CONNECT_CLIENT_ID} \
  --set config.encryption.masterKey=${ENCRYPTION_MASTER_KEY} \
  --set config.email.smtp.password=${SMTP_PASSWORD} \
  --set config.billing.security.jwt.secretKey=${JWT_SECRET_KEY} \
  --namespace iqkv-sit-env

# Production (Tagged releases)
helm upgrade --install --atomic --wait --timeout 5m iqscaffold-billing-service ./ \
  --values ./values.yaml \
  --values ./values-prd.yaml \
  --set image.tag=${DRONE_TAG} \
  --set infraServices.postgresql.password=${INFRA_POSTGRESQL_PASSWORD} \
  --set infraServices.redis.password=${INFRA_REDIS_PASSWORD} \
  --set infraServices.rabbitmq.password=${INFRA_RABBITMQ_PASSWORD} \
  --set config.billing.stripe.publicKey=${STRIPE_PUBLIC_KEY} \
  --set config.billing.stripe.secretKey=${STRIPE_SECRET_KEY} \
  --set config.billing.stripe.webhookSecret=${STRIPE_WEBHOOK_SECRET} \
  --set config.billing.stripe.connectClientId=${STRIPE_CONNECT_CLIENT_ID} \
  --set config.encryption.masterKey=${ENCRYPTION_MASTER_KEY} \
  --set config.email.smtp.password=${SMTP_PASSWORD} \
  --set config.billing.security.jwt.secretKey=${JWT_SECRET_KEY} \
  --namespace iqkv-prd-env
```

</details>

### Manual Deployment

#### Quick Start

```bash
# Clone Helm charts
git clone <HELM_CHARTS_REPOSITORY> charts
cd charts/IQKV/iqscaffold-billing-service

# Deploy to development
helm upgrade --install billing-service ./ \
  --values values-sit.yaml \
  --set infraServices.postgresql.password="your-db-password" \
  --set infraServices.redis.password="your-redis-password" \
  --set infraServices.rabbitmq.password="your-rabbitmq-password" \
  --set config.billing.stripe.publicKey="pk_test_your_stripe_public_key" \
  --set config.billing.stripe.secretKey="sk_test_your_stripe_secret_key" \
  --set config.billing.stripe.webhookSecret="whsec_your_webhook_secret" \
  --set config.billing.stripe.connectClientId="ca_your_connect_client_id" \
  --set config.encryption.masterKey="your-32-char-encryption-key" \
  --set config.email.smtp.password="your-smtp-password" \
  --set config.billing.security.jwt.secretKey="your-secure-symmetric-key" \
  --namespace iqkv-sit-env \
  --create-namespace
```

#### Environment-Specific Deployments

#### Development

```bash
helm upgrade --install billing-service ./ \
  --values values-sit.yaml \
  --set infraServices.postgresql.password="${DB_PASSWORD}" \
  --set infraServices.redis.password="${REDIS_PASSWORD}" \
  --set infraServices.rabbitmq.password="${RABBITMQ_PASSWORD}" \
  --set config.billing.stripe.publicKey="${STRIPE_PUBLIC_KEY}" \
  --set config.billing.stripe.secretKey="${STRIPE_SECRET_KEY}" \
  --set config.billing.stripe.webhookSecret="${STRIPE_WEBHOOK_SECRET}" \
  --set config.billing.stripe.connectClientId="${STRIPE_CONNECT_CLIENT_ID}" \
  --set config.encryption.masterKey="${ENCRYPTION_MASTER_KEY}" \
  --set config.email.smtp.password="${SMTP_PASSWORD}" \
  --set config.billing.security.jwt.secretKey="${JWT_SECRET_KEY}" \
  --namespace iqkv-sit-env \
  --create-namespace
```

#### Production

```bash
helm upgrade --install billing-service ./ \
  --values values-prd.yaml \
  --set infraServices.postgresql.password="${DB_PASSWORD}" \
  --set infraServices.redis.password="${REDIS_PASSWORD}" \
  --set infraServices.rabbitmq.password="${RABBITMQ_PASSWORD}" \
  --set config.billing.stripe.publicKey="${STRIPE_PUBLIC_KEY}" \
  --set config.billing.stripe.secretKey="${STRIPE_SECRET_KEY}" \
  --set config.billing.stripe.webhookSecret="${STRIPE_WEBHOOK_SECRET}" \
  --set config.billing.stripe.connectClientId="${STRIPE_CONNECT_CLIENT_ID}" \
  --set config.encryption.masterKey="${ENCRYPTION_MASTER_KEY}" \
  --set config.email.smtp.password="${SMTP_PASSWORD}" \
  --set config.billing.security.jwt.secretKey="${JWT_SECRET_KEY}" \
  --namespace iqkv-prd-env \
  --create-namespace
```

### Configuration

#### Drone CI Secrets

The following secrets must be configured in Drone CI for automated deployments:

```bash
# Infrastructure Secrets
drone secret add --repository IQKV/iqscaffold-billing-service --name INFRA_POSTGRESQL_PASSWORD --data "your-postgresql-password"
drone secret add --repository IQKV/iqscaffold-billing-service --name INFRA_REDIS_PASSWORD --data "your-redis-password"
drone secret add --repository IQKV/iqscaffold-billing-service --name INFRA_RABBITMQ_PASSWORD --data "your-rabbitmq-password"

# Stripe Payment Secrets
drone secret add --repository IQKV/iqscaffold-billing-service --name STRIPE_PUBLIC_KEY --data "pk_live_your_stripe_public_key"
drone secret add --repository IQKV/iqscaffold-billing-service --name STRIPE_SECRET_KEY --data "sk_live_your_stripe_secret_key"
drone secret add --repository IQKV/iqscaffold-billing-service --name STRIPE_WEBHOOK_SECRET --data "whsec_your_webhook_secret"
drone secret add --repository IQKV/iqscaffold-billing-service --name STRIPE_CONNECT_CLIENT_ID --data "ca_your_connect_client_id"

# Application Secrets
drone secret add --repository IQKV/iqscaffold-billing-service --name ENCRYPTION_MASTER_KEY --data "your-32-char-encryption-master-key"
drone secret add --repository IQKV/iqscaffold-billing-service --name SMTP_PASSWORD --data "your-smtp-password"
drone secret add --repository IQKV/iqscaffold-billing-service --name JWT_SECRET_KEY --data "your-secure-symmetric-key"

```

#### Environment Variable Mapping

The Helm chart maps Drone CI secrets to application environment variables:

| Drone Secret                | Helm --set Parameter                    | Application Environment Variable         |
| --------------------------- | --------------------------------------- | ---------------------------------------- |
| `INFRA_POSTGRESQL_PASSWORD` | `infraServices.postgresql.password`     | `IQSCAFFOLD_DATABASE_PASSWORD`           |
| `INFRA_REDIS_PASSWORD`      | `infraServices.redis.password`          | `IQSCAFFOLD_CACHE_REDIS_PASSWORD`        |
| `INFRA_RABBITMQ_PASSWORD`   | `infraServices.rabbitmq.password`       | `IQSCAFFOLD_MESSAGING_RABBITMQ_PASSWORD` |
| `STRIPE_PUBLIC_KEY`         | `config.billing.stripe.publicKey`       | `STRIPE_PUBLIC_KEY`                      |
| `STRIPE_SECRET_KEY`         | `config.billing.stripe.secretKey`       | `STRIPE_SECRET_KEY`, `STRIPE_API_KEY`    |
| `STRIPE_WEBHOOK_SECRET`     | `config.billing.stripe.webhookSecret`   | `STRIPE_WEBHOOK_SECRET`                  |
| `STRIPE_CONNECT_CLIENT_ID`  | `config.billing.stripe.connectClientId` | `STRIPE_CLIENT_ID`                       |
| `ENCRYPTION_MASTER_KEY`     | `config.encryption.masterKey`           | `GATEWAY_CONFIG_ENCRYPTION_KEY`          |
| `SMTP_PASSWORD`             | `config.email.smtp.password`            | `SMTP_PASSWORD`                          |
| `JWT_SECRET_KEY`            | `config.billing.security.jwt.secretKey` | `JWT_SECRET_KEY`                         |

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
    kubectl logs deployment/iqscaffold-billing-service -n iqkv-sit-env
    ```

2. **Redis Connection Issues**

    ```bash
    # Check Redis connectivity
    kubectl exec -it deployment/iqscaffold-billing-service -n iqkv-sit-env -- \
      redis-cli -h foundation-infra-redis-master.iqkv-sit-env.svc.cluster.local ping
    ```

3. **Stripe Webhook Validation Errors**

    ```bash
    kubectl logs deployment/iqscaffold-billing-service -n iqkv-sit-env | grep "webhook"
    ```

4. **Encryption Key Issues**

    ```bash
    # Check if encryption key is properly configured
    kubectl get secret iqscaffold-billing-service-secrets -n iqkv-sit-env -o jsonpath='{.data.encryption-master-key}' | base64 -d | wc -c
    # Should return 32 or more characters
    ```

5. **Stripe Configuration Issues**

    ```bash
    # Verify Stripe secrets are set
    kubectl get secret iqscaffold-billing-service-secrets -n iqkv-sit-env -o yaml
    ```

6. **Check Configuration**

    ```bash
    kubectl describe configmap iqscaffold-billing-service-config -n iqkv-sit-env
    kubectl describe secret iqscaffold-billing-service-secrets -n iqkv-sit-env
    ```

7. **Test Health Endpoints**
    ```bash
    kubectl port-forward deployment/iqscaffold-billing-service 8081:8081 -n iqkv-sit-env
    curl http://localhost:8081/actuator/health
    ```

#### Missing Secrets Diagnosis

If deployments fail due to missing secrets, check:

```bash
# List all secrets in namespace
kubectl get secrets -n iqkv-sit-env

# Check specific secret content
kubectl get secret iqscaffold-billing-service-secrets -n iqkv-sit-env -o yaml

# Verify Drone CI secrets are configured
drone secret ls --repository IQKV/iqscaffold-billing-service
```

#### Stripe Webhook Testing

```bash
# Test webhook endpoint locally
kubectl port-forward deployment/iqscaffold-billing-service 8080:8080 -n iqkv-sit-env

# Use Stripe CLI to forward webhooks
stripe listen --forward-to localhost:8080/api/v1/billing/webhooks/stripe
```

#### Rollback

```bash
# Rollback to previous version
helm rollback iqscaffold-billing-service -n iqkv-prd-env

# Or uninstall completely
helm uninstall iqscaffold-billing-service -n iqkv-prd-env
```

### Security

- All sensitive values passed via `--set` flags
- TLS enabled in production
- Network policies restrict pod communication
- Non-root container execution
- Read-only root filesystem in production
- Stripe webhook signature validation
- Data encryption at rest using master key
