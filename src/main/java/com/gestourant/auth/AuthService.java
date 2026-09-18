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

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final AuditService audit; private final String adminRegistrationCode;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AuditService audit, @Value("${app.admin-registration-code:}") String adminRegistrationCode) { this.users = users; this.encoder = encoder; this.jwt = jwt; this.audit = audit; this.adminRegistrationCode = adminRegistrationCode; }
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim(); String email = request.email().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está registrado");
        if (users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        Role role = users.count() == 0 ? Role.ADMINISTRADOR : request.role() == Role.ADMINISTRADOR && request.adminCode() != null && !adminRegistrationCode.isBlank() && request.adminCode().equals(adminRegistrationCode) ? Role.ADMINISTRADOR : Role.EMPLEADO;
        if (users.count() > 0 && request.role() == Role.ADMINISTRADOR && role != Role.ADMINISTRADOR) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El código de administrador no es válido");
        User user = users.save(new User(username, email, encoder.encode(request.password()), role));
        log.info("User registered username={} role={}", username, role);
        audit.log(user, "CREATE", "usuarios", "Cuenta creada con rol " + role, null);
        return response(user);
    }
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = users.findByEmailIgnoreCase(identifier).or(() -> users.findByUsernameIgnoreCase(identifier)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        log.info("User authenticated username={} role={}", user.getUsername(), user.getRole());
        audit.log(user, "LOGIN", "sesion", "Inicio de sesi\u00f3n", null);
        return response(user);
    }
    public void logout(User user, String ip) { audit.log(user, "LOGOUT", "sesion", "Cierre de sesi\u00f3n", ip); }
    private AuthResponse response(User user) { return new AuthResponse(jwt.generate(user), "Bearer", user.getId(), user.getUsername(), user.getEmail(), user.getRole()); }
}
