CREATE TABLE oauth_identities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_oauth_provider_subject UNIQUE (provider, subject),
    CONSTRAINT fk_oauth_identities_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
