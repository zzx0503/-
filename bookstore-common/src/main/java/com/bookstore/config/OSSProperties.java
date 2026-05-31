package com.bookstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bookstore.oss")
public class OSSProperties {

    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private String stsRoleArn;
    private Integer stsDurationSeconds;
    private String publicPrefix;
    private String avatarDir;
    private String bookCoverDir;
    private String reviewImageDir;
    private String bookCoverSourceDir;
}
