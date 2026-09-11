package com.gestourant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwt;
    private final com.gestourant.audit.CrudAuditFilter audit;
    public SecurityConfig(JwtAuthenticationFilter jwt, com.gestourant.audit.CrudAuditFilter audit) { this.jwt = jwt; this.audit = audit; }
    @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception { return http.csrf(AbstractHttpConfigurer::disable).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a -> a.requestMatchers("/api/auth/register", "/api/auth/login", "/error").permitAll().requestMatchers("/api/audit-logs/**").hasRole("ADMINISTRADOR").anyRequest().authenticated()).addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).addFilterAfter(audit, JwtAuthenticationFilter.class).build(); }
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
