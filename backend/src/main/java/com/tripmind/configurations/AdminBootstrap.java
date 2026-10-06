package com.tripmind.configurations;

import com.tripmind.enums.UserRole;
import com.tripmind.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cấp vai trò ADMIN cho các email trong {@code ADMIN_EMAILS} (phân tách bằng dấu phẩy) lúc khởi
 * động. Không có API nào tự nâng quyền; tài khoản phải đăng ký trước như người dùng thường.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${tripmind.admin.emails:}")
    private List<String> adminEmails;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (String email : adminEmails) {
            if (email == null || email.isBlank()) {
                continue;
            }
            userRepository.findByEmail(email.trim()).ifPresentOrElse(user -> {
                if (user.getRole() != UserRole.ADMIN) {
                    user.setRole(UserRole.ADMIN);
                    userRepository.save(user);
                    log.info("Cap quyen ADMIN cho {}", email.trim());
                }
            }, () -> log.warn("ADMIN_EMAILS co {} nhung chua co tai khoan nay", email.trim()));
        }
    }
}
