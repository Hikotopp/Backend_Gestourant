package com.gestourant.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.gestourant.user.UserRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final UserRepository users;
    public AuthController(AuthService service, UserRepository users) { this.service = service; this.users = users; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }
    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(@AuthenticationPrincipal UserDetails principal, jakarta.servlet.http.HttpServletRequest request) { users.findByEmailIgnoreCase(principal.getUsername()).ifPresent(user -> service.logout(user, request.getRemoteAddr())); }
}
