package multi_tenant_task_management.multi_tenant_task_management.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import multi_tenant_task_management.multi_tenant_task_management.security.CustomUserDetails;
import multi_tenant_task_management.multi_tenant_task_management.security.CustomUserDetailsService;
import multi_tenant_task_management.multi_tenant_task_management.security.TenantContext;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — runs once per request.
 *
 * Flow:
 *  1. Extract "Authorization: Bearer <token>" header
 *  2. Validate token via JwtUtil
 *  3. Set Spring SecurityContext with authenticated user
 *  4. Set TenantContext (ThreadLocal) with tenant_id from JWT
 *  5. Continue filter chain
 *  6. Clear TenantContext in finally block (prevent memory leaks)
 *
 * tenant_id is NEVER read from the request body or params — only from the JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // --- Step 1: Extract and validate JWT ---
        try {
            final String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                final String jwt = authHeader.substring(7).trim();
                final String email = jwtUtil.extractEmail(jwt);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    CustomUserDetails userDetails =
                            (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);

                    if (jwtUtil.validateToken(jwt, userDetails)) {
                        // Step 2: Set tenant context from JWT (NOT from request)
                        Long tenantId = jwtUtil.extractTenantId(jwt);
                        TenantContext.setTenantId(tenantId);

                        // Step 3: Set security context
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("JWT authentication error: " + e.getMessage());
            // Do not rethrow — let request proceed unauthenticated (Spring Security will reject it)
        }

        // --- Step 4: Continue filter chain, clear tenant context after response ---
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
