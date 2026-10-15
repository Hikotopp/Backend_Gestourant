package com.gestourant.config;

import com.gestourant.auth.JwtService;
import com.gestourant.auth.ActiveSessionService;
import com.gestourant.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.time.LocalDateTime;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final ActiveSessionService sessions;
    public JwtAuthenticationFilter(JwtService jwt, UserRepository users, ActiveSessionService sessions) { this.jwt = jwt; this.users = users; this.sessions = sessions; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims;
            try {
                claims = jwt.parse(header.substring(7));
            } catch (JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }
            users.findByEmailIgnoreCase(claims.getSubject()).ifPresent(account -> {
                String path = request.getServletPath();
                boolean sessionStatusRequest = path.equals("/api/auth/session");
                if (claims.getId() == null) return;
                if (sessionStatusRequest && !claims.getId().equals(account.getActiveSessionId())) {
                    setAuthentication(request, account);
                    return;
                }
                if (!claims.getId().equals(account.getActiveSessionId())
                    || account.getActiveSessionExpiresAt() == null
                    || !account.getActiveSessionExpiresAt().isAfter(LocalDateTime.now())) return;
                boolean sessionControlRequest = sessionStatusRequest
                    || path.equals("/api/auth/unlock")
                    || path.equals("/api/auth/logout");
                if (!sessionControlRequest) {
                    ActiveSessionService.SessionStatus status = sessions.status(account.getId(), claims.getId());
                    if (!status.active()) return;
                    if (status.locked()) {
                        response.setStatus(423);
                        return;
                    }
                }
                setAuthentication(request, account);
            });
        }
        if (response.getStatus() == 423) {
            response.sendError(423, "SESSION_LOCKED");
            return;
        }
        chain.doFilter(request, response);
    }
    private void setAuthentication(HttpServletRequest request, com.gestourant.user.User account) {
        User principal = new User(account.getEmail(), account.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
