package com.iqscaffold.billingservice.tenancy;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TenantLiquibaseRunner {

  private static final Logger logger = LoggerFactory.getLogger(TenantLiquibaseRunner.class);

  private final DataSource dataSource;
  private final String tenantChangeLog;
  private final String systemChangeLog;
  private final String contexts;

  public TenantLiquibaseRunner(
      final DataSource dataSource,
      @Value("${iqscaffold.liquibase.tenantChangeLog:classpath:db/changelog/tenant/master.xml}") final String tenantChangeLog,
      @Value("${iqscaffold.liquibase.systemChangeLog:classpath:db/changelog/system/master.xml}") final String systemChangeLog,
      @Value("${iqscaffold.liquibase.contexts:}") final String contexts) {
    this.dataSource = dataSource;
    this.tenantChangeLog = tenantChangeLog;
    this.systemChangeLog = systemChangeLog;
    this.contexts = contexts;
  }

  // System changelog typically sets up shared tables or the public schema itself if needed
  public void runSystemChangelog() throws Exception {
    logger.info("Running system changelog with contexts: {}", contexts);

    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema("public");
    liquibase.setLiquibaseSchema("public");
    liquibase.setChangeLog(systemChangeLog);

    if (StringUtils.hasText(contexts)) {
      liquibase.setContexts(contexts);
    }

    liquibase.afterPropertiesSet();
  }

  // Runs migrations for a specific tenant schema
  public void runTenantChangelog(String schema) throws Exception {
    logger.info("Running tenant changelog for schema '{}' with contexts: {}", schema, contexts);

    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema(schema);
    liquibase.setLiquibaseSchema(schema);
    liquibase.setChangeLog(tenantChangeLog);

    if (StringUtils.hasText(contexts)) {
      liquibase.setContexts(contexts);
    }

    liquibase.afterPropertiesSet();
  }
}
