package com.bookstore.domain.vo.oss;

import lombok.Data;

@Data
public class STSTokenVO {

    private String accessKeyId;
    private String accessKeySecret;
    private String securityToken;
    private String endpoint;
    private String bucket;
    private String region;
    private Long expiration;
    private String dirPrefix;
}
