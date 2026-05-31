package com.bookstore.domain.vo.address;

import lombok.Data;

@Data
public class AddressVO {

    private Long id;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Boolean isDefault;

    private String fullAddress;
}
