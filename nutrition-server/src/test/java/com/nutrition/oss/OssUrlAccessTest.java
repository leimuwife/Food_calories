package com.nutrition.oss;

import com.aliyun.oss.OSS;
import com.nutrition.config.OssConfig;
import com.nutrition.util.OssUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * OSS 访问地址转换测试。
 * <p>
 * 验证公开 Bucket 原样返回、私有 Bucket 生成短期签名 URL，避免私有化后图片无法访问。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OssUrlAccessTest {

    private static final String BUCKET = "test-bucket";
    private static final String STORED_URL = "https://test-bucket.oss-cn-beijing.aliyuncs.com/2026/09/24/abc.jpg";

    @Mock
    private OSS ossClient;

    private OssConfig ossConfig;
    private OssUtil ossUtil;

    @BeforeEach
    void setUp() {
        ossConfig = new OssConfig();
        ossConfig.setBucketName(BUCKET);
        ossConfig.setEndpoint("oss-cn-beijing.aliyuncs.com");
        ossConfig.setSignedUrlExpireSeconds(3600);
        ossConfig.setPrivateBucket(false);
        ossUtil = new OssUtil(ossConfig, ossClient);
    }

    /** 公开 Bucket 模式：地址原样返回，不生成签名。 */
    @Test
    void shouldReturnStoredUrlWhenBucketIsPublic() {
        assertEquals(STORED_URL, ossUtil.toAccessibleUrl(STORED_URL));
    }

    /** 私有 Bucket 模式：生成带签名的短期 URL，并正确提取对象 key。 */
    @Test
    void shouldGenerateSignedUrlWhenBucketIsPrivate() throws Exception {
        ossConfig.setPrivateBucket(true);
        URL signed = new URL(STORED_URL + "?Expires=1&Signature=abc");
        when(ossClient.generatePresignedUrl(eq(BUCKET), eq("2026/09/24/abc.jpg"), any(Date.class)))
                .thenReturn(signed);

        String result = ossUtil.toAccessibleUrl(STORED_URL);

        assertTrue(result.contains("Signature=abc"), "私有模式应返回签名 URL，实际: " + result);
    }

    /** 空值安全：null / 空串原样返回，不抛异常。 */
    @Test
    void shouldHandleNullOrEmpty() {
        assertNull(ossUtil.toAccessibleUrl(null));
        assertEquals("", ossUtil.toAccessibleUrl(""));
    }

    /** 私有模式但 OSS 客户端未初始化（未配置 endpoint）时，降级返回原地址。 */
    @Test
    void shouldFallbackWhenOssClientIsNull() {
        OssConfig config = new OssConfig();
        config.setBucketName(BUCKET);
        config.setPrivateBucket(true);
        OssUtil utilWithoutClient = new OssUtil(config, null);

        assertEquals(STORED_URL, utilWithoutClient.toAccessibleUrl(STORED_URL));
    }
}
