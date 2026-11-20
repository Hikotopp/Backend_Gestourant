package com.gestourant.user;

import com.gestourant.audit.AuditService;
import com.gestourant.auth.ActiveSessionService;
import com.gestourant.auth.AuthResponse;
import com.gestourant.auth.PasswordPolicy;
import com.gestourant.time.ColombiaTime;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAccountService {
    private static final long MAX_PHOTO_BYTES = 1_048_576;
    private static final int MAX_PHOTO_DIMENSION = 2048;
    private final UserRepository users;
    private final UserProfilePhotoRepository photos;
    private final PasswordEncoder encoder;
    private final ActiveSessionService sessions;
    private final AuditService audit;

    public UserAccountService(
        UserRepository users,
        UserProfilePhotoRepository photos,
        PasswordEncoder encoder,
        ActiveSessionService sessions,
        AuditService audit
    ) {
        this.users = users;
        this.photos = photos;
        this.encoder = encoder;
        this.sessions = sessions;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ManagedUserView> listUsers() {
        return users.findAll(Sort.by(Sort.Direction.ASC, "id")).stream().map(ManagedUserView::from).toList();
    }

    @Transactional
    public ManagedUserView createUser(ManagedUserCreateRequest request, User administrator) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        ensureCredentialsAvailable(username, email, null);
        User user = users.save(new User(username, email, encoder.encode(request.password()), request.role(), ColombiaTime.now()));
        audit.log(administrator, "CREATE", "usuarios", "Cuenta creada para " + username + " con rol " + request.role(), null);
        return ManagedUserView.from(user);
    }

    @Transactional
    public ManagedUserView updateUser(Long id, ManagedUserUpdateRequest request, User administrator) {
        User user = users.findByIdForUpdate(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el usuario."));
        if (user.getId().equals(administrator.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Para proteger el acceso, tu propia cuenta se edita desde Mi perfil.");
        }
        boolean removingAdminAccess = user.isEnabled()
            && user.getRole() == Role.ADMINISTRADOR
            && (!request.enabled() || request.role() != Role.ADMINISTRADOR);
        if (removingAdminAccess) {
            List<User> enabledAdmins = users.findEnabledByRoleForUpdate(Role.ADMINISTRADOR);
            if (enabledAdmins.size() <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe quedar al menos un administrador activo.");
            }
        }
        user.setRole(request.role());
        user.setEnabled(request.enabled());
        audit.log(administrator, "UPDATE", "usuarios", "Se actualizó el acceso de " + user.getUsername() + ": rol " + request.role() + ", activo " + request.enabled(), null);
        return ManagedUserView.from(user);
    }

    @Transactional(readOnly = true)
    public UserProfileView getProfile(String email) {
        User user = findUser(email);
        return UserProfileView.from(user, photos.existsById(user.getId()));
    }

    @Transactional
    public AuthResponse updateProfile(String email, ProfileUpdateRequest request) {
        User user = findUser(email);
        if (!encoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La contraseña actual es incorrecta.");
        }

        String username = request.username().trim();
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        ensureCredentialsAvailable(username, normalizedEmail, user.getId());

        String newPassword = request.newPassword();
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.equals(request.currentPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser diferente a la actual.");
            }
            if (newPassword.length() < PasswordPolicy.MIN_LENGTH
                || newPassword.length() > PasswordPolicy.MAX_LENGTH
                || !newPassword.matches(PasswordPolicy.PATTERN)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordPolicy.MESSAGE);
            }
            user.updatePasswordHash(encoder.encode(newPassword));
        }

        user.updateCredentials(username, normalizedEmail);
        audit.log(user, "UPDATE", "perfil", "Se actualizaron las credenciales de la cuenta.", null);
        return sessions.refresh(user);
    }

    @Transactional
    public UserProfileView savePhoto(String email, MultipartFile file) {
        User user = findUser(email);
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona una foto para subir.");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe pesar máximo 1 MB.");
        }
        byte[] image;
        try {
            image = file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fue posible leer la foto seleccionada.", exception);
        }
        String contentType = validatePhoto(file.getContentType(), image);
        photos.save(new UserProfilePhoto(user.getId(), contentType, image));
        audit.log(user, "UPDATE", "perfil", "Se actualizó la foto de perfil.", null);
        return UserProfileView.from(user, true);
    }

    @Transactional(readOnly = true)
    public UserProfilePhoto getPhoto(String email) {
        User user = findUser(email);
        return photos.findById(user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todavía no tienes una foto de perfil."));
    }

    @Transactional
    public void deletePhoto(String email) {
        User user = findUser(email);
        if (!photos.existsById(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Todavía no tienes una foto de perfil.");
        }
        photos.deleteById(user.getId());
        audit.log(user, "DELETE", "perfil", "Se eliminó la foto de perfil.", null);
    }

    private User findUser(String email) {
        return users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no existe."));
    }

    private void ensureCredentialsAvailable(String username, String email, Long currentUserId) {
        users.findByUsernameIgnoreCase(username).filter(existing -> !existing.getId().equals(currentUserId))
            .ifPresent(existing -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está registrado."); });
        users.findByEmailIgnoreCase(email).filter(existing -> !existing.getId().equals(currentUserId))
            .ifPresent(existing -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado."); });
    }

    private String validatePhoto(String contentType, byte[] image) {
        String normalizedType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!normalizedType.equals("image/jpeg") && !normalizedType.equals("image/png")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe estar en formato JPG o PNG.");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(image))) {
            if (input == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es una imagen JPG o PNG válida.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es una imagen JPG o PNG válida.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                String expectedType = format.equals("jpeg") || format.equals("jpg") ? "image/jpeg"
                    : format.equals("png") ? "image/png" : "";
                if (!normalizedType.equals(expectedType) || width < 1 || height < 1
                    || width > MAX_PHOTO_DIMENSION || height > MAX_PHOTO_DIMENSION
                    || (long) width * height > 4_194_304L) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La foto debe ser JPG o PNG y no superar 2048 × 2048 píxeles.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fue posible procesar la foto seleccionada.");
                return expectedType;
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es una imagen JPG o PNG válida.", exception);
        }
    }
}
