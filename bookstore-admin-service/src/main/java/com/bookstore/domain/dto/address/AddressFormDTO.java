package com.bookstore.domain.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressFormDTO {

    @NotBlank
    @Size(max = 30)
    private String receiver;

    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank
    @Size(max = 20)
    private String province;

    @NotBlank
    @Size(max = 20)
    private String city;

    @NotBlank
    @Size(max = 20)
    private String district;

    @NotBlank
    @Size(max = 100)
    private String detailAddress;

    private Boolean setDefault;
}
