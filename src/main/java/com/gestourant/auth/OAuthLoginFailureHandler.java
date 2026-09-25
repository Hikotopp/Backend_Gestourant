package com.gestourant.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuthLoginFailureHandler implements AuthenticationFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(OAuthLoginFailureHandler.class);
    private final String frontendUrl;

    public OAuthLoginFailureHandler(@Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof OAuth2AuthenticationException oauthException) {
            log.warn("OAuth login failed with provider error code {}", oauthException.getError().getErrorCode());
        } else {
            log.warn("OAuth login failed with exception type {}", exception.getClass().getSimpleName());
        }
        response.sendRedirect(frontendUrl + "/#oauth_error=No%20se%20pudo%20completar%20el%20inicio%20de%20sesi%C3%B3n");
    }
}
