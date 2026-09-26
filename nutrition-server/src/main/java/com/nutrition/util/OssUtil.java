package com.nutrition.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.nutrition.config.OssConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OssUtil {

    private final OssConfig ossConfig;
    private final OSS ossClient;

    public String upload(MultipartFile file) throws IOException {
        return upload(file, null);
    }

    public String upload(MultipartFile file, String customPath) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + suffix;

        String filePath = customPath != null ? customPath + "/" + fileName : datePath + "/" + fileName;

        if (ossClient != null) {
            return uploadToOss(file, filePath);
        } else {
            log.warn("OSS客户端未初始化，文件将保存到本地");
            return uploadToLocal(file, filePath);
        }
    }

    private String uploadToOss(MultipartFile file, String filePath) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(ossConfig.getBucketName(), filePath, inputStream);

            // 私有 Bucket 模式下不再把对象设为公开读，读取时改用短期签名 URL
            if (!ossConfig.isPrivateBucket()) {
                ossClient.setObjectAcl(ossConfig.getBucketName(), filePath, CannedAccessControlList.PublicRead);
            }

            String url;
            if (ossConfig.getDomain() != null && !ossConfig.getDomain().isEmpty()) {
                url = ossConfig.getDomain() + "/" + filePath;
            } else {
                url = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + filePath;
            }

            // 仅记录对象 key，避免完整地址（含域名信息）落盘
            log.info("文件上传OSS成功: key={}", filePath);
            return url;
        } catch (Exception e) {
            log.error("文件上传OSS失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 把数据库中存储的文件地址转换为客户端可访问的地址。
     * <p>
     * 公开 Bucket：原样返回。
     * 私有 Bucket：根据对象 key 生成带有效期的短期签名 URL。
     * </p>
     *
     * @param fileUrl 数据库中保存的文件地址
     * @return 可直接访问的地址
     */
    public String toAccessibleUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return fileUrl;
        }
        if (!ossConfig.isPrivateBucket() || ossClient == null) {
            return fileUrl;
        }
        try {
            String key = extractKeyFromUrl(fileUrl);
            Date expiration = new Date(System.currentTimeMillis() + ossConfig.getSignedUrlExpireSeconds() * 1000L);
            return ossClient.generatePresignedUrl(ossConfig.getBucketName(), key, expiration).toString();
        } catch (Exception e) {
            log.error("生成OSS签名URL失败: error={}", e.getMessage());
            return fileUrl;
        }
    }

    private String uploadToLocal(MultipartFile file, String filePath) throws IOException {
        String localPath = "/upload/" + filePath;
        log.info("文件保存到本地路径: {}", localPath);
        return localPath;
    }

    public void delete(String filePath) {
        if (ossClient != null && filePath != null && !filePath.isEmpty()) {
            try {
                String key = extractKeyFromUrl(filePath);
                if (ossClient.doesObjectExist(ossConfig.getBucketName(), key)) {
                    ossClient.deleteObject(ossConfig.getBucketName(), key);
                    log.info("文件删除OSS成功: key={}", key);
                }
            } catch (Exception e) {
                log.error("文件删除OSS失败: {}", e.getMessage(), e);
            }
        }
    }

    private String extractKeyFromUrl(String url) {
        if (url.contains(ossConfig.getBucketName())) {
            int index = url.indexOf(ossConfig.getBucketName()) + ossConfig.getBucketName().length() + 1;
            if (url.contains(".oss-cn-")) {
                index = url.indexOf("/", index);
                if (index != -1) {
                    return url.substring(index + 1);
                }
            }
        }
        if (ossConfig.getDomain() != null && url.startsWith(ossConfig.getDomain())) {
            return url.substring(ossConfig.getDomain().length() + 1);
        }
        return url;
    }
}
