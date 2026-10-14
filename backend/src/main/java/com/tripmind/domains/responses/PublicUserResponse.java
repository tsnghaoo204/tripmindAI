package com.tripmind.domains.responses;

import com.tripmind.entities.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Thông tin công khai của tác giả trên API công khai: không có email, vai trò hay trạng thái. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserResponse {

    private Long id;
    private String name;
    private String avatarUrl;

    public static PublicUserResponse fromEntity(UserEntity user) {
        if (user == null) {
            return null;
        }
        return PublicUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
