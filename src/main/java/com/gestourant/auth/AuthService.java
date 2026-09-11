package com.gestourant.auth;

import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import com.gestourant.user.Role;
import com.gestourant.audit.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt; private final AuditService audit;
    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AuditService audit) { this.users = users; this.encoder = encoder; this.jwt = jwt; this.audit = audit; }
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim(); String email = request.email().trim().toLowerCase();
        if (users.existsByUsernameIgnoreCase(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está registrado");
        if (users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        Role role = users.count() == 0 ? Role.ADMINISTRADOR : Role.EMPLEADO;
        User user = users.save(new User(username, email, encoder.encode(request.password()), role));
        audit.log(user, "CREATE", "usuarios", "Cuenta creada con rol " + role, null);
        return response(user);
    }
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        User user = users.findByEmailIgnoreCase(identifier).or(() -> users.findByUsernameIgnoreCase(identifier)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));
        if (!encoder.matches(request.password(), user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        audit.log(user, "LOGIN", "sesion", "Inicio de sesi\u00f3n", null);
        return response(user);
    }
    public void logout(User user, String ip) { audit.log(user, "LOGOUT", "sesion", "Cierre de sesi\u00f3n", ip); }
    private AuthResponse response(User user) { return new AuthResponse(jwt.generate(user), "Bearer", user.getId(), user.getUsername(), user.getEmail(), user.getRole()); }
}
