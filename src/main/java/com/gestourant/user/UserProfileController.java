package com.gestourant.user;

import com.gestourant.auth.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {
    private final UserAccountService accounts;

    public UserProfileController(UserAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    public UserProfileView get(@AuthenticationPrincipal UserDetails principal) {
        return accounts.getProfile(principal.getUsername());
    }

    @PutMapping
    public AuthResponse update(
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return accounts.updateProfile(principal.getUsername(), request);
    }

    @PutMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileView uploadPhoto(
        @AuthenticationPrincipal UserDetails principal,
        @RequestParam("file") MultipartFile file
    ) {
        return accounts.savePhoto(principal.getUsername(), file);
    }

    @GetMapping("/photo")
    public ResponseEntity<byte[]> getPhoto(@AuthenticationPrincipal UserDetails principal) {
        UserProfilePhoto photo = accounts.getPhoto(principal.getUsername());
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.getContentType()))
            .cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .body(photo.getImageData());
    }

    @DeleteMapping("/photo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(@AuthenticationPrincipal UserDetails principal) {
        accounts.deletePhoto(principal.getUsername());
    }
}
