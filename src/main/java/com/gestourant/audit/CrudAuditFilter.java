package com.gestourant.audit;

import com.gestourant.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

@Component
public class CrudAuditFilter extends OncePerRequestFilter {
    private static final Map<String, String> ACTIONS = Map.of("POST", "CREATE", "PUT", "UPDATE", "PATCH", "UPDATE", "DELETE", "DELETE");
    private final UserRepository users;
    private final AuditService audit;
    public CrudAuditFilter(UserRepository users, AuditService audit) { this.users = users; this.audit = audit; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return !ACTIONS.containsKey(request.getMethod()) || !request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/api/auth/"); }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (response.getStatus() < 400 && authentication != null && authentication.isAuthenticated()) {
            users.findByEmailIgnoreCase(authentication.getName()).ifPresent(user -> audit.log(user, ACTIONS.get(request.getMethod()), request.getRequestURI(), "Operaci\u00f3n completada", request.getRemoteAddr()));
        }
    }
}
