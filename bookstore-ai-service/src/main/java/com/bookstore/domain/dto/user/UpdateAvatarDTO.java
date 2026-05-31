package com.bookstore.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateAvatarDTO {

    @NotBlank
    @Size(max = 200)
    @Pattern(regexp = "^avatars/.+", message = "avatarKey 必须以 avatars/ 开头")
    private String avatarKey;
}
