package com.gestourant.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthCodeStore {
    private static final long CODE_LIFETIME_SECONDS = 60;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Entry> codes = new ConcurrentHashMap<>();

    public String issue(AuthResponse response) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        purgeExpired();
        codes.put(code, new Entry(response, Instant.now().plusSeconds(CODE_LIFETIME_SECONDS)));
        return code;
    }

    public AuthResponse consume(String code) {
        if (code == null || code.isBlank()) return null;
        Entry entry = codes.remove(code);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) return null;
        return entry.response();
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record Entry(AuthResponse response, Instant expiresAt) {}
}
