package com.gestourant.auth;

import com.gestourant.audit.AuditService;
import com.gestourant.user.Role;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class OAuthAccountService {
    private final UserRepository users;
    private final OAuthIdentityRepository identities;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public OAuthAccountService(UserRepository users, OAuthIdentityRepository identities, PasswordEncoder encoder, JwtService jwt, AuditService audit) {
        this.users = users;
        this.identities = identities;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
    }

    @Transactional
    public Optional<AuthResponse> authenticateExisting(String provider, String subject) {
        String normalizedProvider = provider.toLowerCase(Locale.ROOT);
        return identities.findByProviderAndSubject(normalizedProvider, subject)
            .map(OAuthIdentity::getUser)
            .map(user -> {
                audit.log(user, "LOGIN", "sesion", "Inicio de sesión con " + normalizedProvider, null);
                return responseFor(user);
            });
    }

    @Transactional
    public AuthResponse registerWithOAuth(String provider, String subject, String email, String displayName, boolean privacyConsent) {
        if (!privacyConsent) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Autoriza el tratamiento de datos antes de crear una cuenta.");
        }
        String normalizedProvider = provider.toLowerCase(Locale.ROOT);
        Optional<AuthResponse> existing = authenticateExisting(normalizedProvider, subject);
        if (existing.isPresent()) return existing.get();
        User user = createAccount(normalizedProvider, email, displayName, subject);
        audit.log(user, "LOGIN", "sesion", "Inicio de sesión con " + normalizedProvider, null);
        return responseFor(user);
    }

    private User createAccount(String provider, String email, String displayName, String subject) {
        if (email == null || email.isBlank() || email.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El proveedor no entregó un correo válido");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo. Inicia sesión con ella para continuar.");
        }

        String base = (displayName == null || displayName.isBlank() ? normalizedEmail.substring(0, normalizedEmail.indexOf('@')) : displayName)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9_.-]", ".")
            .replaceAll("\\.+", ".");
        base = base.replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
        if (base.length() < 3) base = "usuario";
        base = base.substring(0, Math.min(base.length(), 38));
        String username = base;
        while (users.existsByUsernameIgnoreCase(username)) {
            username = base + "." + java.util.UUID.randomUUID().toString().substring(0, 6);
        }

        byte[] passwordBytes = new byte[48];
        random.nextBytes(passwordBytes);
        String randomPassword = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(passwordBytes);
        Role role = users.count() == 0 ? Role.ADMINISTRADOR : Role.EMPLEADO;
        User user = users.save(new User(username, normalizedEmail, encoder.encode(randomPassword), role, LocalDateTime.now()));
        identities.save(new OAuthIdentity(user, provider, subject));
        audit.log(user, "CREATE", "usuarios", "Cuenta creada con " + provider, null);
        return user;
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwt.generate(user), "Bearer", user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
