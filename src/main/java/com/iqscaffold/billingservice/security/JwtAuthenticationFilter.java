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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String TENANT_ID_HEADER = "X-Tenant-ID";
  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Add correlation ID to MDC for distributed tracing
    String correlationId = request.getHeader(CORRELATION_ID_HEADER);
    if (correlationId != null) {
      org.slf4j.MDC.put("correlationId", correlationId);
    }

    // Log all relevant headers for debugging
    logger.debug("Request headers - X-Tenant-ID: {}, X-User-ID: {}, X-Correlation-ID: {}, Authorization: {}",
        request.getHeader("X-Tenant-ID"),
        request.getHeader("X-User-ID"),
        request.getHeader("X-Correlation-ID"),
        request.getHeader("Authorization") != null ? "present" : "absent");

    try {
      // Extract user context from JWT if available
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication instanceof JwtAuthenticationToken jwtAuthToken) {
        Jwt jwt = jwtAuthToken.getToken();
        UserContext userContext = extractUserContext(jwt);

        // Set tenant context with priority: X-Tenant-ID header > JWT claim
        String tenantId = extractTenantId(request, userContext);
        if (tenantId != null) {
          TenantContext.setCurrentTenantId(tenantId);
          logger.info("Tenant context set to: {}", tenantId);
        }

        // Add user context to MDC for structured logging
        if (userContext.userId() != null) {
          org.slf4j.MDC.put("userId", userContext.userId().toString());
        }
        if (userContext.username() != null) {
          org.slf4j.MDC.put("username", userContext.username());
        }

        // Create new authentication with UserContext as principal
        // This allows @AuthenticationPrincipal UserContext and SecurityContextHelper to work
        var authorities = userContext.authorities().stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
        
        var newAuth = new UsernamePasswordAuthenticationToken(
            userContext,  // principal
            jwt,          // credentials
            authorities   // authorities
        );
        newAuth.setDetails(jwtAuthToken.getDetails());
        
        SecurityContextHolder.getContext().setAuthentication(newAuth);
        
        logger.debug("User context set as authentication principal - userId: {}, username: {}, tenantId: {}", 
            userContext.userId(), userContext.username(), userContext.tenantId());
      } else {
        logger.warn("No JWT authentication found, authentication type: {}", 
            authentication != null ? authentication.getClass().getSimpleName() : "null");
      }

      filterChain.doFilter(request, response);
    } finally {
      // Clear context to prevent memory leaks in thread pool
      TenantContext.clear();
      org.slf4j.MDC.clear();
    }
  }

  /**
   * Extract tenant ID with priority: X-Tenant-ID header > JWT claim.
   *
   * @param request     the HTTP request
   * @param userContext the user context extracted from JWT
   * @return the tenant ID or null if not found
   */
  private String extractTenantId(HttpServletRequest request, UserContext userContext) {
    // Priority 1: X-Tenant-ID header (from Gateway)
    String tenantId = request.getHeader(TENANT_ID_HEADER);
    if (StringUtils.hasText(tenantId)) {
      logger.info("Tenant ID extracted from X-Tenant-ID header: {}", tenantId);
      return tenantId.trim();
    }

    // Priority 2: JWT tenant_id claim
    if (userContext.tenantId() != null) {
      logger.info("Tenant ID extracted from JWT: {}", userContext.tenantId());
      return userContext.tenantId();
    }

    logger.warn("No tenant ID found in X-Tenant-ID header or JWT claim");
    return null;
  }

  /**
   * Extract user context from JWT claims with fallback support for different claim formats.
   *
   * @param jwt the JWT token
   * @return the user context
   */
  private UserContext extractUserContext(Jwt jwt) {
    // Try to extract user ID from multiple possible claims
    Long userId = extractLong(jwt.getClaim(JwtClaimNames.SUBJECT));
    if (userId == null) {
      // Fallback to userId claim if sub is not available
      userId = extractLong(jwt.getClaim(JwtClaimNames.USER_ID));
    }
    
    String username = jwt.getClaim(JwtClaimNames.USERNAME);
    String email = jwt.getClaim(JwtClaimNames.EMAIL);
    Set<String> authorities = extractAuthorities(jwt.getClaim(JwtClaimNames.AUTHORITIES));
    String tenantId = jwt.getClaim(JwtClaimNames.TENANT_ID);
    Long organizationId = extractLong(jwt.getClaim(JwtClaimNames.ORGANIZATION_ID));
    String firstName = jwt.getClaim(JwtClaimNames.FIRST_NAME);
    String lastName = jwt.getClaim(JwtClaimNames.LAST_NAME);

    logger.debug("Extracted user context - userId: {}, username: {}, tenantId: {}", userId, username, tenantId);

    return new UserContext(userId, username, email, authorities, tenantId, organizationId, firstName, lastName);
  }

  /**
   * Extract Long value from JWT claim, handling various numeric types.
   *
   * @param value the claim value
   * @return the Long value or null
   */
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

  /**
   * Extract authorities from JWT claim, handling List or Set types.
   *
   * @param value the claim value
   * @return the set of authorities
   */
  @SuppressWarnings("unchecked")
  private Set<String> extractAuthorities(Object value) {
    return switch (value) {
      case List<?> list -> new HashSet<>(list.stream().map(Object::toString).toList());
      case Set<?> set -> new HashSet<>(set.stream().map(Object::toString).toList());
      case null, default -> Collections.emptySet();
    };
  }
}
