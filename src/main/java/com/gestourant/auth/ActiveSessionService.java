package com.gestourant.auth;

import com.gestourant.audit.AuditService;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ActiveSessionService {
    private static final Duration LOCK_AFTER_INACTIVITY = Duration.ofSeconds(150);
    private static final Duration CLOSE_AFTER_INACTIVITY = Duration.ofSeconds(300);
    private final UserRepository users;
    private final JwtService jwt;
    private final AuditService audit;
    private final long tokenLifetimeMillis;

    public ActiveSessionService(
        UserRepository users,
        JwtService jwt,
        AuditService audit,
        @Value("${jwt.expiration}") long tokenLifetimeMillis
    ) {
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
        boolean previousSessionClosed = user.getActiveSessionId() != null
            && user.getActiveSessionExpiresAt() != null
            && user.getActiveSessionExpiresAt().isAfter(now);

        if (previousSessionClosed) {
            audit.log(
                user,
                "SESSION_REPLACED",
                "sesion",
                "La sesión anterior se invalidó al iniciar una nueva sesión.",
                null
            );
        }

        String sessionId = UUID.randomUUID().toString();
        user.setActiveSession(sessionId, now.plusNanos(tokenLifetimeMillis * 1_000_000), now);
        return new AuthResponse(
            jwt.generate(user, sessionId),
            "Bearer",
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            previousSessionClosed
        );
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

    @Transactional
    public SessionStatus status(Long userId, String sessionId) {
        User user = users.findByIdForUpdate(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no existe."));
        return evaluate(user, sessionId, LocalDateTime.now());
    }

    @Transactional
    public SessionStatus recordActivity(Long userId, String sessionId) {
        User user = users.findByIdForUpdate(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no existe."));
        LocalDateTime now = LocalDateTime.now();
        SessionStatus status = evaluate(user, sessionId, now);
        if (!status.active()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión expiró por inactividad.");
        }
        if (status.locked()) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "La sesión está bloqueada. Verifica tu contraseña para continuar.");
        }
        user.setLastActivityAt(now);
        return statusFor(user, now);
    }

    @Transactional
    public SessionStatus unlock(User requestedUser, String sessionId) {
        User user = users.findByIdForUpdate(requestedUser.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no existe."));
        LocalDateTime now = LocalDateTime.now();
        SessionStatus status = evaluate(user, sessionId, now);
        if (!status.active()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión expiró por inactividad.");
        }
        user.setActiveSessionLocked(false);
        user.setLastActivityAt(now);
        audit.log(user, "SESSION_UNLOCKED", "sesion", "Sesión reanudada después de validar la contraseña.", null);
        return statusFor(user, now);
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void closeInactiveSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minus(LOCK_AFTER_INACTIVITY);
        for (Long userId : users.findActiveSessionIdsInactiveSince(cutoff)) {
            users.findByIdForUpdate(userId).ifPresent(user -> {
                if (user.getActiveSessionId() != null && user.getLastActivityAt() != null
                    && !user.getLastActivityAt().isAfter(cutoff)) {
                    evaluate(user, user.getActiveSessionId(), now);
                }
            });
        }
    }

    private SessionStatus evaluate(User user, String sessionId, LocalDateTime now) {
        if (sessionId == null || !sessionId.equals(user.getActiveSessionId())) {
            return SessionStatus.inactive(user.getActiveSessionId() == null ? "SESSION_EXPIRED" : "SESSION_REPLACED");
        }
        if (user.getActiveSessionExpiresAt() == null || !user.getActiveSessionExpiresAt().isAfter(now)) {
            user.clearActiveSession();
            audit.log(user, "SESSION_EXPIRED", "sesion", "La sesión expiró.", null);
            return SessionStatus.inactive("SESSION_EXPIRED");
        }
        if (user.getLastActivityAt() == null) {
            throw new IllegalStateException("La sesión activa no tiene registrada su última actividad.");
        }

        long inactiveMillis = Math.max(0, Duration.between(user.getLastActivityAt(), now).toMillis());
        if (inactiveMillis >= CLOSE_AFTER_INACTIVITY.toMillis()) {
            user.clearActiveSession();
            audit.log(user, "SESSION_CLOSED_INACTIVITY", "sesion", "Sesión cerrada tras cinco minutos de inactividad.", null);
            return SessionStatus.inactive("INACTIVITY_TIMEOUT");
        }
        if (inactiveMillis >= LOCK_AFTER_INACTIVITY.toMillis() && !user.isActiveSessionLocked()) {
            user.setActiveSessionLocked(true);
            audit.log(user, "SESSION_LOCKED_INACTIVITY", "sesion", "Sesión bloqueada tras dos minutos y medio de inactividad.", null);
        }
        return statusFor(user, now);
    }

    private SessionStatus statusFor(User user, LocalDateTime now) {
        long inactiveMillis = Math.max(0, Duration.between(user.getLastActivityAt(), now).toMillis());
        long secondsUntilLock = user.isActiveSessionLocked()
            ? 0 : remainingSeconds(LOCK_AFTER_INACTIVITY, inactiveMillis);
        return new SessionStatus(
            true,
            user.isActiveSessionLocked(),
            secondsUntilLock,
            remainingSeconds(CLOSE_AFTER_INACTIVITY, inactiveMillis),
            user.isActiveSessionLocked() ? "SESSION_LOCKED" : null
        );
    }

    private long remainingSeconds(Duration limit, long elapsedMillis) {
        long remainingMillis = Math.max(0, limit.toMillis() - elapsedMillis);
        return (remainingMillis + 999) / 1000;
    }

    public record SessionStatus(boolean active, boolean locked, long secondsUntilLock, long secondsUntilLogout, String reason) {
        private static SessionStatus inactive(String reason) {
            return new SessionStatus(false, false, 0, 0, reason);
        }
    }
}
