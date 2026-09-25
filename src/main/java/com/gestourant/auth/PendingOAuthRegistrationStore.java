package com.gestourant.auth;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingOAuthRegistrationStore {
    private static final long CODE_LIFETIME_SECONDS = 300;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, Entry> registrations = new ConcurrentHashMap<>();

    public String issue(String provider, String subject, String email, String displayName) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        purgeExpired();
        registrations.put(code, new Entry(new Profile(provider, subject, email, displayName), Instant.now().plusSeconds(CODE_LIFETIME_SECONDS)));
        return code;
    }

    public Profile consume(String code) {
        if (code == null || code.isBlank()) return null;
        Entry entry = registrations.remove(code);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) return null;
        return entry.profile();
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        registrations.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    public record Profile(String provider, String subject, String email, String displayName) {}
    private record Entry(Profile profile, Instant expiresAt) {}
}
