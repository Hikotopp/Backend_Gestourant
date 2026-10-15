package com.gestourant.auth;

import com.gestourant.audit.AuditService;
import com.gestourant.user.Role;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActiveSessionServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final AuditService audit = mock(AuditService.class);
    private final ActiveSessionService service = new ActiveSessionService(users, jwt, audit, 86_400_000L);

    @Test
    void replacesThePreviousSessionInsteadOfRejectingTheNewLogin() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getActiveSessionId()).thenReturn("old-session");
        when(user.getActiveSessionExpiresAt()).thenReturn(LocalDateTime.now().plusHours(1));
        when(user.getUsername()).thenReturn("maria");
        when(user.getEmail()).thenReturn("maria@example.com");
        when(user.getRole()).thenReturn(Role.EMPLEADO);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));
        when(jwt.generate(any(User.class), any(String.class))).thenReturn("new-token");

        AuthResponse response = service.open(user);

        assertTrue(response.previousSessionClosed());
        verify(user).setActiveSession(any(String.class), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(audit).log(eq(user), org.mockito.ArgumentMatchers.eq("SESSION_REPLACED"),
            org.mockito.ArgumentMatchers.eq("sesion"), contains("anterior"), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void locksTheSessionAfterTwoAndAHalfMinutesOfInactivity() {
        User user = inactiveUser("session-1", 151);
        when(users.findActiveSessionIdsInactiveSince(any(LocalDateTime.class))).thenReturn(List.of(7L));
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));

        service.closeInactiveSessions();

        verify(user).setActiveSessionLocked(true);
        verify(audit).log(eq(user), eq("SESSION_LOCKED_INACTIVITY"), eq("sesion"),
            contains("dos minutos y medio"), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void closesTheSessionAfterFiveMinutesOfInactivity() {
        User user = inactiveUser("session-1", 301);
        when(users.findActiveSessionIdsInactiveSince(any(LocalDateTime.class))).thenReturn(List.of(7L));
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));

        service.closeInactiveSessions();

        verify(user).clearActiveSession();
        verify(audit).log(eq(user), eq("SESSION_CLOSED_INACTIVITY"), eq("sesion"),
            contains("cinco minutos"), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void unlocksTheExistingSessionAndResetsItsInactivityTimer() {
        User user = mock(User.class);
        LocalDateTime now = LocalDateTime.now();
        when(user.getId()).thenReturn(7L);
        when(user.getActiveSessionId()).thenReturn("session-1");
        when(user.getActiveSessionExpiresAt()).thenReturn(now.plusHours(1));
        when(user.getLastActivityAt()).thenReturn(now.minusMinutes(3), now);
        when(user.isActiveSessionLocked()).thenReturn(true, false);
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));

        ActiveSessionService.SessionStatus status = service.unlock(user, "session-1");

        assertTrue(status.active());
        assertFalse(status.locked());
        assertTrue(status.secondsUntilLogout() <= 300);
        verify(user).setActiveSessionLocked(false);
        verify(user).setLastActivityAt(any(LocalDateTime.class));
        verify(user, never()).setActiveSession(any(String.class), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(jwt, never()).generate(any(User.class), any(String.class));
        verify(audit).log(eq(user), eq("SESSION_UNLOCKED"), eq("sesion"),
            contains("validar la contraseña"), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    void reportsWhenTheActiveSessionWasReplaced() {
        User user = mock(User.class);
        when(user.getActiveSessionId()).thenReturn("new-session");
        when(users.findByIdForUpdate(7L)).thenReturn(Optional.of(user));

        ActiveSessionService.SessionStatus status = service.status(7L, "old-session");

        assertFalse(status.active());
        assertFalse(status.locked());
        org.junit.jupiter.api.Assertions.assertEquals("SESSION_REPLACED", status.reason());
    }

    private User inactiveUser(String sessionId, long secondsInactive) {
        User user = mock(User.class);
        LocalDateTime now = LocalDateTime.now();
        when(user.getId()).thenReturn(7L);
        when(user.getActiveSessionId()).thenReturn(sessionId);
        when(user.getActiveSessionExpiresAt()).thenReturn(now.plusHours(1));
        when(user.getLastActivityAt()).thenReturn(now.minusSeconds(secondsInactive));
        when(user.isActiveSessionLocked()).thenReturn(false);
        return user;
    }
}
