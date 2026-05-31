package com.bookstore.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bookstore.domain.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("address")
public class Address extends BaseEntity {

    @TableField("user_id")
    private Long userId;

    private String receiver;
    private String phone;

    private String province;
    private String city;
    private String district;

    @TableField("detail_address")
    private String detailAddress;

    @TableField("is_default")
    private Integer isDefault;
}
