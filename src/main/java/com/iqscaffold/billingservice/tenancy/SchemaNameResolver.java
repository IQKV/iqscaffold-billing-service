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
  private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
  private static final Pattern ILLEGAL = Pattern.compile("[^a-z0-9_]");

  /**
   * Constructor with configurable schema prefix.
   *
   * @param prefix the schema prefix (default: "tenant_")
   */
  public SchemaNameResolver(@Value("${iqscaffold.tenancy.schema.prefix:tenant_}") final String prefix) {
    this.prefix = prefix;
  }

  /**
   * Convert tenant ID to schema name.
   *
   * @param tenantId the tenant identifier
   * @return the schema name (e.g., "tenant_default") or "public" if tenant ID is null/blank
   */
  public String toSchema(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return "public";
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
