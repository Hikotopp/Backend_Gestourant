package com.gestourant.user;

import com.gestourant.audit.AuditService;
import com.gestourant.auth.ActiveSessionService;
import com.gestourant.auth.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserAccountServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserProfilePhotoRepository photos = mock(UserProfilePhotoRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final ActiveSessionService sessions = mock(ActiveSessionService.class);
    private final AuditService audit = mock(AuditService.class);
    private final UserAccountService service = new UserAccountService(users, photos, encoder, sessions, audit);

    @Test
    void doesNotAllowDeactivatingTheLastAdministrator() {
        User administrator = mock(User.class);
        User lastAdmin = mock(User.class);
        when(administrator.getId()).thenReturn(1L);
        when(lastAdmin.getId()).thenReturn(2L);
        when(lastAdmin.isEnabled()).thenReturn(true);
        when(lastAdmin.getRole()).thenReturn(Role.ADMINISTRADOR);
        when(users.findByIdForUpdate(2L)).thenReturn(Optional.of(lastAdmin));
        when(users.findEnabledByRoleForUpdate(Role.ADMINISTRADOR)).thenReturn(List.of(lastAdmin));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
            service.updateUser(2L, new ManagedUserUpdateRequest(Role.EMPLEADO, false), administrator));

        assertEquals(409, error.getStatusCode().value());
        verify(lastAdmin, never()).setRole(any());
        verify(lastAdmin, never()).setEnabled(false);
    }

    @Test
    void requiresTheCurrentPasswordBeforeChangingAccountCredentials() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);
        when(user.getPasswordHash()).thenReturn("stored-hash");
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
            service.updateProfile("user@example.com", new ProfileUpdateRequest(
                "newname", "new@example.com", "wrong-password", null
            )));

        assertEquals(403, error.getStatusCode().value());
        verify(user, never()).updateCredentials(any(), any());
        verify(sessions, never()).refresh(any());
    }

    @Test
    void rejectsMalformedProfileImagesInsteadOfPersistingThem() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(5L);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[] { 1, 2, 3 });

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () ->
            service.savePhoto("user@example.com", file));

        assertEquals(400, error.getStatusCode().value());
        verify(photos, never()).save(any(UserProfilePhoto.class));
    }

    @Test
    void letsAnAdministratorChooseTheRoleWhenCreatingAManagedAccount() {
        User administrator = mock(User.class);
        when(users.findByUsernameIgnoreCase("supervisor")).thenReturn(Optional.empty());
        when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode("ValidPassword1!")).thenReturn("encoded");

        ManagedUserView created = service.createUser(
            new ManagedUserCreateRequest("supervisor", "staff@example.com", "ValidPassword1!", Role.ADMINISTRADOR),
            administrator
        );

        assertEquals(Role.ADMINISTRADOR, created.role());
        assertEquals("staff@example.com", created.email());
        verify(users).save(any(User.class));
        verify(audit).log(eq(administrator), eq("CREATE"), eq("usuarios"), any(), eq(null));
    }
}
