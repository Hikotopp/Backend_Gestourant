package com.gestourant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.gestourant.auth.OAuthLoginSuccessHandler;
import com.gestourant.auth.OAuthLoginFailureHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwt;
    private final com.gestourant.audit.CrudAuditFilter audit;
    private final OAuthLoginSuccessHandler oauthSuccess;
    private final OAuthLoginFailureHandler oauthFailure;
    private final String frontendUrl;
    public SecurityConfig(JwtAuthenticationFilter jwt, com.gestourant.audit.CrudAuditFilter audit, OAuthLoginSuccessHandler oauthSuccess, OAuthLoginFailureHandler oauthFailure, @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) { this.jwt = jwt; this.audit = audit; this.oauthSuccess = oauthSuccess; this.oauthFailure = oauthFailure; this.frontendUrl = frontendUrl; }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(a -> a.requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/config", "/api/auth/oauth/exchange", "/api/auth/oauth/register", "/api/guest/**", "/error", "/oauth2/**", "/login/oauth2/**").permitAll().requestMatchers("/api/audit-logs/**").hasRole("ADMINISTRADOR").anyRequest().authenticated())
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(audit, JwtAuthenticationFilter.class);
        http.oauth2Login(oauth -> oauth
            .successHandler(oauthSuccess)
            .failureHandler(oauthFailure));
        return http.build();
    }
    @Bean CorsConfigurationSource corsConfigurationSource() {
        URI frontend = URI.create(frontendUrl);
        String frontendOrigin = frontend.getScheme() + "://" + frontend.getAuthority();
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> allowedOrigins = new ArrayList<>(List.of(frontendOrigin));
        if ("localhost".equalsIgnoreCase(frontend.getHost()) && "5173".equals(frontend.getPort() < 0 ? "" : Integer.toString(frontend.getPort()))) {
            allowedOrigins.add(frontend.getScheme() + "://127.0.0.1:" + frontend.getPort());
        } else if ("127.0.0.1".equals(frontend.getHost()) && "5173".equals(frontend.getPort() < 0 ? "" : Integer.toString(frontend.getPort()))) {
            allowedOrigins.add(frontend.getScheme() + "://localhost:" + frontend.getPort());
        }
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
