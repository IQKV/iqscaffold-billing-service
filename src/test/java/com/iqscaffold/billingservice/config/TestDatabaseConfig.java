package com.iqscaffold.billingservice.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Test database configuration that provides H2-compatible naming strategy.
 * This configuration removes schema prefixes from table names for H2 compatibility.
 */
@TestConfiguration
public class TestDatabaseConfig {

  @Bean
  public PhysicalNamingStrategy physicalNamingStrategy() {
    return new H2CompatiblePhysicalNamingStrategy();
  }

  /**
   * Custom naming strategy that ignores schema names for H2 compatibility.
   * In H2, we don't want to use schema prefixes like "public." which work in PostgreSQL.
   */
  static class H2CompatiblePhysicalNamingStrategy implements PhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
      return name;
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
      // Return null to ignore schema names in H2
      return null;
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
      return name;
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
      return name;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
      return name;
    }
  }
}
