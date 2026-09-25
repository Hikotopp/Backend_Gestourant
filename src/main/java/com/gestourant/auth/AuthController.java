package com.gestourant.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.gestourant.user.UserRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    private final OAuthAccountService oauthAccountService;
    private final UserRepository users;
    private final TurnstileService turnstile;
    private final OAuthCodeStore oauthCodes;
    private final PendingOAuthRegistrationStore pendingOAuthRegistrations;
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    private final String turnstileSiteKey;
    public AuthController(AuthService service, OAuthAccountService oauthAccountService, UserRepository users, TurnstileService turnstile, OAuthCodeStore oauthCodes, PendingOAuthRegistrationStore pendingOAuthRegistrations, ObjectProvider<ClientRegistrationRepository> registrations, @org.springframework.beans.factory.annotation.Value("${app.turnstile.site-key:}") String turnstileSiteKey) { this.service = service; this.oauthAccountService = oauthAccountService; this.users = users; this.turnstile = turnstile; this.oauthCodes = oauthCodes; this.pendingOAuthRegistrations = pendingOAuthRegistrations; this.registrations = registrations; this.turnstileSiteKey = turnstileSiteKey; }
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) public AuthResponse register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }
    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
    @GetMapping("/config") public AuthConfig config() {
        ClientRegistrationRepository repository = registrations.getIfAvailable();
        return new AuthConfig(turnstile.isEnabled(), turnstile.isEnabled() ? turnstileSiteKey : "", hasRegistration(repository, "google"), hasRegistration(repository, "microsoft"));
    }
    @PostMapping("/oauth/exchange") public AuthResponse exchange(@Valid @RequestBody OAuthExchangeRequest request) {
        AuthResponse result = oauthCodes.consume(request.code());
        if (result == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "El inicio de sesión expiró. Inténtalo de nuevo.");
        return result;
    }
    @PostMapping("/oauth/register") public AuthResponse registerOAuth(@Valid @RequestBody OAuthRegistrationRequest request) {
        PendingOAuthRegistrationStore.Profile profile = pendingOAuthRegistrations.consume(request.code());
        if (profile == null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "El registro expiró. Inicia de nuevo con Google o Microsoft.");
        return oauthAccountService.registerWithOAuth(profile.provider(), profile.subject(), profile.email(), profile.displayName(), request.privacyConsent());
    }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout(@AuthenticationPrincipal UserDetails principal, jakarta.servlet.http.HttpServletRequest request) { users.findByEmailIgnoreCase(principal.getUsername()).ifPresent(user -> service.logout(user, request.getRemoteAddr())); }
    private boolean hasRegistration(ClientRegistrationRepository repository, String id) { return repository != null && repository.findByRegistrationId(id) != null; }
    public record AuthConfig(boolean captchaEnabled, String captchaSiteKey, boolean googleEnabled, boolean microsoftEnabled) {}
    public record OAuthExchangeRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String code) {}
    public record OAuthRegistrationRequest(
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100) String code,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.AssertTrue Boolean privacyConsent
    ) {}
}
