package com.gestourant.auth;

import com.gestourant.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "oauth_identities", uniqueConstraints = {
    @UniqueConstraint(name = "uk_oauth_provider_subject", columnNames = {"provider", "subject"})
})
public class OAuthIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String provider;

    @Column(nullable = false, length = 200)
    private String subject;

    protected OAuthIdentity() {}

    public OAuthIdentity(User user, String provider, String subject) {
        this.user = user;
        this.provider = provider;
        this.subject = subject;
    }

    public User getUser() { return user; }
}
