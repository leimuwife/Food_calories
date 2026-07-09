package com.nutrition;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.InputStream;

@SpringBootTest
public class DefaultImageUploadTest {

    // 从yml读取OSS配置，不要static
    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Test
    public void uploadDefaultFoodImage() {
        String localFilePath = "src/test/resources/default-food-placeholder.png";
        String ossObjectName = "default/food-placeholder.png";

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try (InputStream inputStream = new FileInputStream(localFilePath)) {
            ossClient.putObject(bucketName, ossObjectName, inputStream);
            String imageUrl = String.format("https://%s.%s/%s", bucketName, endpoint, ossObjectName);

            System.out.println("==============================");
            System.out.println("默认图片OSS地址：" + imageUrl);
            System.out.println("yml配置项：nutrition.food.default-image: " + imageUrl);
            System.out.println("==============================");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ossClient.shutdown();
        }
    }
}