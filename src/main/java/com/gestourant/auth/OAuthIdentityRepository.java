package com.gestourant.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OAuthIdentityRepository extends JpaRepository<OAuthIdentity, Long> {
    Optional<OAuthIdentity> findByProviderAndSubject(String provider, String subject);
}
