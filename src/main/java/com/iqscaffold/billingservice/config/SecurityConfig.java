package com.iqscaffold.billingservice.config;

import com.iqscaffold.billingservice.security.JwtAuthenticationFilter;
import com.iqscaffold.billingservice.security.JwtClaimNames;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final IqScaffoldProperties iqScaffoldProperties;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(final IqScaffoldProperties iqScaffoldProperties,
                        final JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.iqScaffoldProperties = iqScaffoldProperties;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher(EndpointRequest.toAnyEndpoint())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(EndpointRequest.to("health", "info", "prometheus")).permitAll()
            .anyRequest().authenticated())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/error",
                "/api/v1/billing/webhooks/**",
                "/api/v1/billing/subscription-plans/active",
                "/api/v1/billing/subscription-plans",
                "/api/v1/billing/payments/health",
                "/api/v1/billing/subscriptions/health",
                "/api/v1/billing/invoices/health",
                "/api/v1/billing/gateway/health")
            .permitAll()
            // Administrative endpoints (defense in depth besides @PreAuthorize)
            .requestMatchers("/api/v1/admin/**").hasAnyAuthority("ADMIN", "SUPER_ADMIN", "BILLING_ADMIN")
            .requestMatchers("/api/v1/billing/subscription-plans/**")
            .hasAnyAuthority("ADMIN", "SUPER_ADMIN", "BILLING_ADMIN")
            // All other requests require authentication
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
            .decoder(jwtDecoder())
            .jwtAuthenticationConverter(jwtAuthenticationConverter())))
        // JWT Authentication Filter (after BearerTokenAuthenticationFilter to extract UserContext)
        // BearerTokenAuthenticationFilter is added by oauth2ResourceServer() configuration
        .addFilterAfter(jwtAuthenticationFilter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class)
        .build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    var jwtProps = iqScaffoldProperties.billing().security().jwt();

    if (isSymmetricConfigured()) {
      var algorithm = jwtProps.algorithm() != null ? jwtProps.algorithm() : "HS256";
      javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(
          jwtProps.secretKey().getBytes(), "Hmac" + algorithm.substring(2));
      return NimbusJwtDecoder.withSecretKey(key).build();
    }

    var jwkSetUri = jwtProps.jwkSetUri();
    if (jwkSetUri == null || jwkSetUri.isBlank()) {
      throw new IllegalStateException("Neither secret-key nor jwk-set-uri is configured for JWT validation");
    }
    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
  }

  private boolean isSymmetricConfigured() {
    var secretKey = iqScaffoldProperties.billing().security().jwt().secretKey();
    return secretKey != null && !secretKey.isBlank() && !"change-me-in-production".equals(secretKey);
  }

  @Bean
  public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
    var grantedAuthoritiesConverter = new org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter();
    grantedAuthoritiesConverter.setAuthoritiesClaimName(JwtClaimNames.AUTHORITIES);
    grantedAuthoritiesConverter.setAuthorityPrefix(""); // usage: authorities are strict

    var jwtAuthenticationConverter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
    return jwtAuthenticationConverter;
  }
}
