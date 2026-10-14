package com.tripmind.entities;

import com.tripmind.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /** Ghi vào JWT; tăng lên thì mọi phiếu đã cấp trước đó hết hiệu lực. */
    @Column(name = "token_version", nullable = false)
    @Builder.Default
    private int tokenVersion = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Đổi mật khẩu (tự đổi hay quản trị đặt lại) thì thu hồi mọi phiên đang mở. */
    public void setPasswordHash(String passwordHash) {
        if (this.passwordHash != null && !this.passwordHash.equals(passwordHash)) {
            tokenVersion++;
        }
        this.passwordHash = passwordHash;
    }

    /** Khoá tài khoản thì thu hồi mọi phiên; mở lại cũng không làm sống lại phiếu cũ. */
    public void setActive(boolean active) {
        if (this.isActive && !active) {
            tokenVersion++;
        }
        this.isActive = active;
    }
}
