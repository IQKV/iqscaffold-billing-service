package com.iqscaffold.billingservice.tenancy;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Schema Name Resolver for multi-tenant database schema management.
 *
 * <p>Converts tenant IDs to valid PostgreSQL schema names following these rules:
 * <ul>
 *   <li>Adds configurable prefix (default: "tenant_")</li>
 *   <li>Converts to lowercase</li>
 *   <li>Replaces hyphens with underscores</li>
 *   <li>Removes illegal characters</li>
 *   <li>Truncates to 48 characters max</li>
 *   <li>Caches results for performance</li>
 * </ul>
 *
 * <h3>Examples:</h3>
 * <pre>
 * "default"     → "tenant_default"
 * "acme-corp"   → "tenant_acme_corp"
 * "ACME-Corp"   → "tenant_acme_corp"
 * "test@123"    → "tenant_test123"
 * null/blank    → "public"
 * </pre>
 *
 * <h3>Configuration:</h3>
 * <pre>
 * iqscaffold:
 *   tenancy:
 *     schema:
 *       prefix: tenant_  # Default prefix
 * </pre>
 *
 * @see SchemaTenantIdentifierResolver
 */
@Component
public class SchemaNameResolver {

  private final String prefix;
  private final String defaultTenantId;
  private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
  private static final Pattern ILLEGAL = Pattern.compile("[^a-z0-9_]");

  /**
   * Constructor with configurable schema prefix and default tenant.
   *
   * @param prefix the schema prefix (default: "tenant_")
   * @param defaultTenantId the default tenant ID to use when tenant context is missing
   */
  public SchemaNameResolver(
      @Value("${iqscaffold.tenancy.schema.prefix:tenant_}") final String prefix,
      @Value("${iqscaffold.bootstrap.default-tenant-schema.tenant-id:default}") final String defaultTenantId) {
    this.prefix = prefix;
    this.defaultTenantId = defaultTenantId;
  }

  /**
   * Convert tenant ID to schema name.
   *
   * @param tenantId the tenant identifier
   * @return the schema name (e.g., "tenant_default")
   */
  public String toSchema(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      // Use default tenant instead of public schema
      // Special case: if default tenant is "PUBLIC", return it as-is without prefix
      if ("PUBLIC".equalsIgnoreCase(defaultTenantId)) {
        return "PUBLIC";
      }
      return cache.computeIfAbsent(defaultTenantId, this::normalize);
    }
    // Special case: if tenant is explicitly "PUBLIC", return it as-is
    if ("PUBLIC".equalsIgnoreCase(tenantId)) {
      return "PUBLIC";
    }
    return cache.computeIfAbsent(tenantId, this::normalize);
  }

  /**
   * Normalize tenant ID to valid schema name.
   *
   * @param tenantId the tenant identifier
   * @return normalized schema name
   */
  private String normalize(String tenantId) {
    var lower = tenantId.toLowerCase(Locale.ROOT).trim();
    var replaced = lower.replace('-', '_');
    var cleaned = ILLEGAL.matcher(replaced).replaceAll("");
    if (cleaned.length() > 48) {
      cleaned = cleaned.substring(0, 48);
    }
    return prefix + cleaned;
  }

  /**
   * Get the configured schema prefix.
   *
   * @return the schema prefix
   */
  public String getPrefix() {
    return prefix;
  }

  /**
   * Clear the schema name cache.
   * Useful for testing or when schema naming rules change.
   */
  public void clearCache() {
    cache.clear();
  }
}
