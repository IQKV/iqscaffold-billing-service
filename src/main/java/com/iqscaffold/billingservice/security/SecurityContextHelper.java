package com.iqscaffold.billingservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Helper class to retrieve the current user context from the security context.
 * 
 * <p>This class provides utility methods to access the authenticated user's context
 * information from the Spring Security context. The UserContext is set as the principal
 * by the JwtAuthenticationFilter after JWT validation.
 * 
 * <h3>Usage:</h3>
 * <pre>
 * UserContext userContext = SecurityContextHelper.getCurrentUserContext();
 * Long userId = SecurityContextHelper.getCurrentUserId();
 * String tenantId = SecurityContextHelper.getCurrentTenantId();
 * </pre>
 */
public final class SecurityContextHelper {

  private SecurityContextHelper() {
  }

  /**
   * Get the current user context from the security context.
   * 
   * <p>Priority:
   * <ol>
   *   <li>SecurityContext authentication principal (set by JwtAuthenticationFilter)</li>
   *   <li>Request attribute "userContext" (backward compatibility)</li>
   * </ol>
   *
   * @return the current user context or null if not authenticated
   */
  public static UserContext getCurrentUserContext() {
    // Priority 1: Get from SecurityContext (standard approach)
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserContext uc) {
      return uc;
    }

    // Priority 2: Fallback to request attribute (backward compatibility)
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes != null) {
      Object userContext = attributes.getRequest().getAttribute("userContext");
      if (userContext instanceof UserContext uc) {
        return uc;
      }
    }
    
    return null;
  }

  /**
   * Get the current user context or throw an exception if not found.
   *
   * @return the current user context
   * @throws IllegalStateException if user context is not found
   */
  public static UserContext getCurrentUserContextOrThrow() {
    UserContext context = getCurrentUserContext();
    if (context == null) {
      throw new IllegalStateException("User context not found in request");
    }
    return context;
  }

  /**
   * Get the current user ID.
   *
   * @return the current user ID or null if not authenticated
   */
  public static Long getCurrentUserId() {
    UserContext uc = getCurrentUserContext();
    return uc != null ? uc.userId() : null;
  }

  /**
   * Get the current tenant ID.
   *
   * @return the current tenant ID or null if not authenticated
   */
  public static String getCurrentTenantId() {
    UserContext uc = getCurrentUserContext();
    return uc != null ? uc.tenantId() : null;
  }
}
