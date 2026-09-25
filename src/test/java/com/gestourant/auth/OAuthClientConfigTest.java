package com.gestourant.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OAuthClientConfigTest {

    private final OAuthClientConfig config = new OAuthClientConfig();

    @Test
    void configuresGoogleSigningKeys() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("app.oauth.google.client-id", "google-client")
            .withProperty("app.oauth.google.client-secret", "google-secret");

        ClientRegistration registration = config.clientRegistrationRepository(environment)
            .findByRegistrationId("google");

        assertNotNull(registration);
        assertEquals("https://www.googleapis.com/oauth2/v3/certs",
            registration.getProviderDetails().getJwkSetUri());
    }

    @Test
    void configuresMicrosoftSigningKeys() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("app.oauth.microsoft.client-id", "microsoft-client")
            .withProperty("app.oauth.microsoft.client-secret", "microsoft-secret");

        ClientRegistration registration = config.clientRegistrationRepository(environment)
            .findByRegistrationId("microsoft");

        assertNotNull(registration);
        assertEquals("https://login.microsoftonline.com/common/discovery/v2.0/keys",
            registration.getProviderDetails().getJwkSetUri());
    }
}
