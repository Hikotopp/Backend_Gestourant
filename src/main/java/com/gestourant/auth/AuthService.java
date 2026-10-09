package com.gestourant.auth;

import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import com.gestourant.user.Role;
import com.gestourant.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository users; private final PasswordEncoder encoder; private final ActiveSessionService sessions; private final AuditService audit; private final String adminRegistrationCode; private final TurnstileService turnstile;
    public AuthService(UserRepository users, PasswordEncoder encoder, ActiveSessionService sessions, AuditService audit, TurnstileService turnstile, @Value("${app.admin-registration-code:}") String adminRegistrationCode) { this.users = users; this.encoder = encoder; this.sessions = sessions; this.audit = audit; this.turnstile = turnstile; this.adminRegistrationCode = adminRegistrationCode; }
    public AuthResponse register(RegisterRequest request) {
        checkBot(request.website(), request.captchaToken());
        String username = request.username().trim(); String email = request.email().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está registrado");
        if (users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        Role role = users.count() == 0 ? Role.ADMINISTRADOR : request.role() == Role.ADMINISTRADOR && request.adminCode() != null && !adminRegistrationCode.isBlank() && request.adminCode().equals(adminRegistrationCode) ? Role.ADMINISTRADOR : Role.EMPLEADO;
        if (users.count() > 0 && request.role() == Role.ADMINISTRADOR && role != Role.ADMINISTRADOR) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El código de administrador no es válido");
        User user = users.save(new User(username, email, encoder.encode(request.password()), role, LocalDateTime.now()));
        log.info("User registered username={} role={}", username, role);
        audit.log(user, "CREATE", "usuarios", "Cuenta creada con rol " + role, null);
        return sessions.open(user);
    }
    public AuthResponse login(LoginRequest request) {
        checkBot(request.website(), request.captchaToken());
        String identifier = request.identifier().trim();
        User user = users.findByEmailIgnoreCase(identifier).or(() -> users.findByUsernameIgnoreCase(identifier)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        log.info("User authenticated username={} role={}", user.getUsername(), user.getRole());
        AuthResponse response = sessions.open(user);
        audit.log(user, "LOGIN", "sesion", "Inicio de sesión", null);
        return response;
    }
    public void logout(User user, String sessionId, String ip) { sessions.close(user, sessionId, ip); }
    private void checkBot(String website, String captchaToken) {
        if (website != null && !website.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fue posible validar la solicitud");
        turnstile.verify(captchaToken);
    }
}
