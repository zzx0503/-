package com.bookstore.domain.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 32, message = "密码长度 6-32")
    private String newPassword;
}
