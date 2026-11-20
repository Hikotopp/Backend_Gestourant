package com.gestourant.user;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminUsersController {
    private final UserAccountService accounts;
    private final UserRepository users;

    public AdminUsersController(UserAccountService accounts, UserRepository users) {
        this.accounts = accounts;
        this.users = users;
    }

    @GetMapping
    public List<ManagedUserView> list() {
        return accounts.listUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ManagedUserView create(
        @Valid @RequestBody ManagedUserCreateRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        User administrator = users.findByEmailIgnoreCase(principal.getUsername()).orElseThrow();
        return accounts.createUser(request, administrator);
    }

    @PatchMapping("/{id}")
    public ManagedUserView update(
        @PathVariable Long id,
        @Valid @RequestBody ManagedUserUpdateRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        User administrator = users.findByEmailIgnoreCase(principal.getUsername()).orElseThrow();
        return accounts.updateUser(id, request, administrator);
    }
}
