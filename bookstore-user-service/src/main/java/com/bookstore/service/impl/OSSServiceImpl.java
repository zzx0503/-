package com.bookstore.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import com.bookstore.config.OSSProperties;
import com.bookstore.domain.vo.oss.STSTokenVO;
import com.bookstore.service.OSSService;
import com.bookstore.utils.OssUrlBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OSSServiceImpl implements OSSService {

    private final OSSProperties ossProps;
    private final OssUrlBuilder ossUrlBuilder;

    @Override
    public STSTokenVO getUploadToken(Long userId, String type) {
        String dirPrefix = resolveDirPrefix(userId, type);

        String policy = buildPolicy(dirPrefix);

        try {
            IClientProfile profile = DefaultProfile.getProfile(
                extractRegion(ossProps.getEndpoint()),
                ossProps.getAccessKeyId(),
                ossProps.getAccessKeySecret()
            );
            DefaultAcsClient client = new DefaultAcsClient(profile);

            AssumeRoleRequest request = new AssumeRoleRequest();
            request.setSysRegionId(extractRegion(ossProps.getEndpoint()));
            request.setRoleArn(ossProps.getStsRoleArn());
            request.setRoleSessionName("bookstore-" + userId + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            request.setPolicy(policy);
            request.setDurationSeconds(ossProps.getStsDurationSeconds().longValue());

            AssumeRoleResponse response = client.getAcsResponse(request);
            AssumeRoleResponse.Credentials credentials = response.getCredentials();

            STSTokenVO vo = new STSTokenVO();
            vo.setAccessKeyId(credentials.getAccessKeyId());
            vo.setAccessKeySecret(credentials.getAccessKeySecret());
            vo.setSecurityToken(credentials.getSecurityToken());
            vo.setEndpoint(ossProps.getEndpoint());
            vo.setBucket(ossProps.getBucket());
            vo.setRegion(extractRegion(ossProps.getEndpoint()));
            vo.setExpiration(Instant.parse(credentials.getExpiration()).getEpochSecond());
            vo.setDirPrefix(dirPrefix);
            return vo;
        } catch (ClientException e) {
            log.error("OSS STS AssumeRole failed", e);
            throw new RuntimeException("获取上传凭证失败: " + e.getMessage());
        }
    }

    @Override
    public void uploadFile(String key, InputStream content, long size, String contentType) {
        OSS client = new OSSClientBuilder().build(
            "https://" + ossProps.getEndpoint(),
            ossProps.getAccessKeyId(),
            ossProps.getAccessKeySecret()
        );
        try {
            ObjectMetadata meta = new ObjectMetadata();
            if (size > 0) {
                meta.setContentLength(size);
            }
            if (contentType != null && !contentType.isEmpty()) {
                meta.setContentType(contentType);
            }
            client.putObject(ossProps.getBucket(), key, content, meta);
        } catch (Exception e) {
            log.error("OSS upload failed: key={}", key, e);
            throw new RuntimeException("OSS 上传失败: " + e.getMessage(), e);
        } finally {
            client.shutdown();
        }
    }

    @Override
    public String upload(MultipartFile file) {
        try {
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
            String key = ossProps.getBookCoverDir() + dir + "/" + UUID.randomUUID() + ext;
            uploadFile(key, file.getInputStream(), file.getSize(), file.getContentType());
            return ossUrlBuilder.toFullUrl(key);
        } catch (Exception e) {
            log.error("OSS upload failed", e);
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择头像文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new RuntimeException("头像大小不能超过2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("请上传图片文件");
        }
        try {
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            }
            String key = ossProps.getAvatarDir() + userId + "/" + UUID.randomUUID() + ext;
            uploadFile(key, file.getInputStream(), file.getSize(), contentType);
            return key;
        } catch (Exception e) {
            log.error("Avatar upload failed", e);
            throw new RuntimeException("头像上传失败: " + e.getMessage(), e);
        }
    }

    private String resolveDirPrefix(Long userId, String type) {
        if ("avatar".equals(type)) {
            return ossProps.getAvatarDir() + userId + "/";
        }
        if ("bookCover".equals(type)) {
            return ossProps.getBookCoverDir();
        }
        if ("review".equals(type)) {
            return ossProps.getReviewImageDir() + userId + "/";
        }
        return "tmp/" + userId + "/";
    }

    private String buildPolicy(String dirPrefix) {
        return "{"
            + "  \"Version\": \"1\","
            + "  \"Statement\": [{"
            + "    \"Effect\": \"Allow\","
            + "    \"Action\": [\"oss:PutObject\"],"
            + "    \"Resource\": [\"acs:oss:*:*:" + ossProps.getBucket() + "/" + dirPrefix + "*\"]"
            + "  }]"
            + "}";
    }

    private String extractRegion(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return "cn-hangzhou";
        }
        String domain = endpoint;
        if (domain.contains(":")) {
            domain = domain.split(":")[0];
        }
        if (domain.startsWith("oss-")) {
            int dotIndex = domain.indexOf('.');
            if (dotIndex > 0) {
                return domain.substring(4, dotIndex);
            }
        }
        return "cn-hangzhou";
    }
}
