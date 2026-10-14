package com.tripmind.configurations;

import com.tripmind.entities.UserEntity;
import com.tripmind.enums.UserRole;
import com.tripmind.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Tạo tài khoản ADMIN cho các email trong {@code ADMIN_EMAILS} lúc khởi động.
 *
 * <p>Hệ thống không xác minh email, nên bất kỳ ai cũng có thể đăng ký trước một địa chỉ trong
 * danh sách. Vì vậy:
 * <ul>
 *   <li>email chưa có tài khoản: tạo mới với mật khẩu {@code ADMIN_BOOTSTRAP_PASSWORD}
 *       (thiếu hoặc ngắn hơn 12 ký tự thì bỏ qua và ghi lỗi);</li>
 *   <li>email đã có tài khoản thường: KHÔNG nâng quyền, trừ khi đặt rõ
 *       {@code ADMIN_BOOTSTRAP_PROMOTE_EXISTING=true} sau khi đã tự kiểm tra chủ tài khoản.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${tripmind.admin.emails:}")
    private List<String> adminEmails;

    @Value("${tripmind.admin.bootstrap-password:}")
    private String bootstrapPassword;

    @Value("${tripmind.admin.promote-existing:false}")
    private boolean promoteExisting;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String raw : adminEmails) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String email = raw.trim().toLowerCase(Locale.ROOT);
            userRepository.findByEmail(email).ifPresentOrElse(
                    user -> promote(user, email),
                    () -> create(email));
        }
    }

    private void promote(UserEntity user, String email) {
        if (user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (!promoteExisting) {
            log.warn("ADMIN_EMAILS co {} nhung tai khoan nay da duoc dang ky nhu nguoi dung thuong; KHONG nang quyen."
                    + " Khong co xac minh email nen bat ky ai cung co the dang ky truoc dia chi nay de chiem quyen ADMIN."
                    + " Neu da kiem tra chu tai khoan, dat ADMIN_BOOTSTRAP_PROMOTE_EXISTING=true roi khoi dong lai.", email);
            return;
        }
        log.warn("ADMIN_BOOTSTRAP_PROMOTE_EXISTING=true: nang quyen ADMIN cho tai khoan da ton tai {} (id={})",
                email, user.getId());
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
    }

    private void create(String email) {
        if (bootstrapPassword == null || bootstrapPassword.length() < MIN_PASSWORD_LENGTH) {
            log.error("ADMIN_EMAILS co {} nhung ADMIN_BOOTSTRAP_PASSWORD trong hoac ngan hon {} ky tu; bo qua tao ADMIN",
                    email, MIN_PASSWORD_LENGTH);
            return;
        }
        UserEntity admin = userRepository.save(UserEntity.builder()
                .email(email)
                .name("Admin")
                .passwordHash(passwordEncoder.encode(bootstrapPassword))
                .role(UserRole.ADMIN)
                .isActive(true)
                .build());
        log.info("Tao tai khoan ADMIN {} (id={}) tu ADMIN_EMAILS", email, admin.getId());
    }
}
