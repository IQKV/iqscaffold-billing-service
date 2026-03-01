package com.iqscaffold.billingservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.iqscaffold.billingservice.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String correlationId = request.getHeader("X-Correlation-ID");
    if (correlationId != null) {
      org.slf4j.MDC.put("correlationId", correlationId);
    }

    String tenantId = null;

    // Log all relevant headers for debugging
    logger.debug("Request headers - X-Tenant-ID: {}, X-User-ID: {}, X-Correlation-ID: {}, Authorization: {}",
        request.getHeader("X-Tenant-ID"),
        request.getHeader("X-User-ID"),
        request.getHeader("X-Correlation-ID"),
        request.getHeader("Authorization") != null ? "present" : "absent");

    // Priority 1: Extract tenant ID from JWT token
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
      Jwt jwt = jwtAuthToken.getToken();
      UserContext userContext = extractUserContext(jwt);
      tenantId = userContext.tenantId();
      request.setAttribute("userContext", userContext);
      
      if (tenantId != null && !tenantId.trim().isEmpty()) {
        logger.info("Tenant ID extracted from JWT: {}", tenantId);
      } else {
        logger.warn("JWT token present but no tenant_id claim found");
      }
    } else {
      logger.warn("No JWT authentication found, authentication type: {}", 
          authentication != null ? authentication.getClass().getSimpleName() : "null");
    }

    // Priority 2: Fallback to X-Tenant-ID header (sent by gateway)
    if (tenantId == null || tenantId.trim().isEmpty()) {
      String headerTenantId = request.getHeader("X-Tenant-ID");
      if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
        tenantId = headerTenantId.trim();
        logger.info("Tenant ID extracted from X-Tenant-ID header: {}", tenantId);
      } else {
        logger.warn("X-Tenant-ID header is missing or empty");
      }
    }

    // Set tenant context if available
    if (tenantId != null && !tenantId.trim().isEmpty()) {
      TenantContext.setCurrentTenantId(tenantId);
      logger.info("Tenant context set to: {}", tenantId);
    } else {
      logger.error("CRITICAL: No tenant context available - neither JWT claim nor X-Tenant-ID header present for request: {} {}",
          request.getMethod(), request.getRequestURI());
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
      org.slf4j.MDC.clear();
    }
  }

  private UserContext extractUserContext(Jwt jwt) {
    Long userId = extractLong(jwt.getClaim(JwtClaimNames.SUBJECT));
    String username = jwt.getClaim(JwtClaimNames.USERNAME);
    String email = jwt.getClaim(JwtClaimNames.EMAIL);
    Set<String> authorities = extractAuthorities(jwt.getClaim(JwtClaimNames.AUTHORITIES));
    String tenantId = jwt.getClaim(JwtClaimNames.TENANT_ID);
    Long organizationId = extractLong(jwt.getClaim(JwtClaimNames.ORGANIZATION_ID));
    String firstName = jwt.getClaim(JwtClaimNames.FIRST_NAME);
    String lastName = jwt.getClaim(JwtClaimNames.LAST_NAME);

    return new UserContext(userId, username, email, authorities, tenantId, organizationId, firstName, lastName);
  }

  private Long extractLong(Object value) {
    return switch (value) {
      case Long l -> l;
      case Integer i -> i.longValue();
      case String s -> {
        try {
          yield Long.parseLong(s);
        } catch (final NumberFormatException e) {
          yield null;
        }
      }
      case null, default -> null;
    };
  }

  @SuppressWarnings("unchecked")
  private Set<String> extractAuthorities(Object value) {
    return switch (value) {
      case List<?> list -> new HashSet<>(list.stream().map(Object::toString).toList());
      case Set<?> set -> new HashSet<>(set.stream().map(Object::toString).toList());
      case null, default -> Collections.emptySet();
    };
  }
}
