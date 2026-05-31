package com.bookstore.domain.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileDTO {

    @Size(max = 30, message = "昵称不能超过 30 字")
    private String nickname;

    @Min(value = 0, message = "gender 仅 0/1/2")
    @Max(value = 2, message = "gender 仅 0/1/2")
    private Integer gender;

    private LocalDate birthday;
}
