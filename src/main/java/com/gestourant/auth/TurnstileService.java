package com.gestourant.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class TurnstileService {
    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
    private final String secretKey;
    private final RestClient restClient = RestClient.create();

    public TurnstileService(@Value("${app.turnstile.secret-key:}") String secretKey, @Value("${app.turnstile.site-key:}") String siteKey) {
        this.secretKey = secretKey;
        if (secretKey.isBlank() != siteKey.isBlank()) {
            throw new IllegalStateException("Configura juntas TURNSTILE_SITE_KEY y TURNSTILE_SECRET_KEY");
        }
    }

    public boolean isEnabled() {
        return !secretKey.isBlank();
    }

    public void verify(String token) {
        if (!isEnabled()) return;
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa la verificación de seguridad");
        }

        var form = new LinkedMultiValueMap<String, String>();
        form.add("secret", secretKey);
        form.add("response", token);

        try {
            Map<?, ?> response = restClient.post()
                .uri(VERIFY_URL)
                .body(form)
                .retrieve()
                .body(Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La verificación de seguridad no fue válida");
            }
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No fue posible verificar la seguridad de la solicitud", ex);
        }
    }
}
