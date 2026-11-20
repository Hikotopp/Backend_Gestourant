package com.gestourant.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OAuthClientConfig {
    @Bean
    ClientRegistrationRepository clientRegistrationRepository(Environment environment) {
        List<ClientRegistration> registrations = new ArrayList<>();
        addGoogle(environment, registrations);
        return registrationId -> registrations.stream()
            .filter(registration -> registration.getRegistrationId().equals(registrationId))
            .findFirst()
            .orElse(null);
    }

    private void addGoogle(Environment environment, List<ClientRegistration> registrations) {
        String clientId = environment.getProperty("app.oauth.google.client-id", "");
        String clientSecret = environment.getProperty("app.oauth.google.client-secret", "");
        if (clientId.isBlank() || clientSecret.isBlank()) return;

        registrations.add(ClientRegistration.withRegistrationId("google")
            .clientId(clientId)
            .clientSecret(clientSecret)
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
            .userNameAttributeName("sub")
            .clientName("Google")
            .build());
    }
}
