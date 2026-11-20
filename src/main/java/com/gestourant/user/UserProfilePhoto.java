package com.gestourant.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import com.gestourant.time.ColombiaTime;

@Entity
@Table(name = "user_profile_photos")
public class UserProfilePhoto {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "content_type", nullable = false, length = 32)
    private String contentType;

    @Lob
    @Column(name = "image_data", nullable = false, columnDefinition = "MEDIUMBLOB")
    private byte[] imageData;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserProfilePhoto() { }

    public UserProfilePhoto(Long userId, String contentType, byte[] imageData) {
        this.userId = userId;
        this.contentType = contentType;
        this.imageData = imageData;
        this.updatedAt = ColombiaTime.now();
    }

    public Long getUserId() { return userId; }
    public String getContentType() { return contentType; }
    public byte[] getImageData() { return imageData; }
}
