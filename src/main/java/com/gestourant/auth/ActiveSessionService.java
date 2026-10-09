package com.gestourant.auth;

import com.gestourant.audit.AuditService;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ActiveSessionService {
    private final UserRepository users;
    private final JwtService jwt;
    private final AuditService audit;
    private final long tokenLifetimeMillis;

    public ActiveSessionService(UserRepository users, JwtService jwt, AuditService audit,
                                @Value("${jwt.expiration}") long tokenLifetimeMillis) {
        this.users = users;
        this.jwt = jwt;
        this.audit = audit;
        this.tokenLifetimeMillis = tokenLifetimeMillis;
    }

    @Transactional
    public AuthResponse open(User requestedUser) {
        User user = users.findByIdForUpdate(requestedUser.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no existe."));
        LocalDateTime now = LocalDateTime.now();
        if (user.getActiveSessionId() != null && user.getActiveSessionExpiresAt() != null
            && user.getActiveSessionExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Esta cuenta ya tiene una sesión activa. Cierra la sesión en el otro navegador o espera a que expire.");
        }

        String sessionId = UUID.randomUUID().toString();
        user.setActiveSession(sessionId, now.plusNanos(tokenLifetimeMillis * 1_000_000));
        return new AuthResponse(jwt.generate(user, sessionId), "Bearer", user.getId(),
            user.getUsername(), user.getEmail(), user.getRole());
    }

    @Transactional
    public void close(User requestedUser, String sessionId, String ip) {
        User user = users.findByIdForUpdate(requestedUser.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no existe."));
        if (sessionId != null && sessionId.equals(user.getActiveSessionId())) {
            user.clearActiveSession();
            audit.log(user, "LOGOUT", "sesion", "Cierre de sesión", ip);
        }
    }
}
