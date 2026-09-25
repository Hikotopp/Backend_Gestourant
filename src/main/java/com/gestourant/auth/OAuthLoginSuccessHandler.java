package com.gestourant.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final OAuthAccountService accounts;
    private final OAuthCodeStore codes;
    private final PendingOAuthRegistrationStore pendingRegistrations;
    private final String frontendUrl;

    public OAuthLoginSuccessHandler(OAuthAccountService accounts, OAuthCodeStore codes, PendingOAuthRegistrationStore pendingRegistrations, @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.accounts = accounts;
        this.codes = codes;
        this.pendingRegistrations = pendingRegistrations;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
            OAuth2User principal = oauth.getPrincipal();
            Map<String, Object> attributes = principal.getAttributes();
            String email = text(attributes.get("email"));
            if (email == null) email = text(attributes.get("preferred_username"));
            String displayName = text(attributes.get("name"));
            String provider = oauth.getAuthorizedClientRegistrationId();
            var existingAccount = accounts.authenticateExisting(provider, principal.getName());
            if (existingAccount.isEmpty()) {
                String registrationCode = pendingRegistrations.issue(provider, principal.getName(), email, displayName);
                response.sendRedirect(frontendUrl + "/#oauth_registration=" + URLEncoder.encode(registrationCode, StandardCharsets.UTF_8));
                return;
            }
            AuthResponse authResponse = existingAccount.get();
            String code = codes.issue(authResponse);
            response.sendRedirect(frontendUrl + "/#oauth_code=" + URLEncoder.encode(code, StandardCharsets.UTF_8));
        } catch (ResponseStatusException ex) {
            response.sendRedirect(frontendUrl + "/#oauth_error=" + URLEncoder.encode(ex.getReason() == null ? "oauth_failed" : ex.getReason(), StandardCharsets.UTF_8));
        }
    }

    private String text(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
