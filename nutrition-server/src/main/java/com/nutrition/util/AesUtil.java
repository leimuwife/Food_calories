package com.nutrition.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES 加密工具类
 * 使用 AES-GCM 模式，提供 API 密钥等敏感信息的加解密功能
 */
@Component
@Slf4j
public class AesUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_LENGTH = 16;

    @Value("${ai.aes.key:NutritionAI2024AesKey32ByteLength!}")
    private String aesKey;

    private SecretKeySpec secretKeySpec;

    @PostConstruct
    public void init() {
        if (aesKey == null || aesKey.isEmpty()) {
            throw new IllegalArgumentException("AES key must not be empty");
        }
        byte[] key = deriveKey(aesKey);
        this.secretKeySpec = new SecretKeySpec(key, ALGORITHM);
        String keyHex = bytesToHex(key);
        log.info("AES encryption utility initialized, key: {}...", keyHex.substring(0, 8));
    }

    private byte[] deriveKey(String password) {
        byte[] keyBytes = password.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < AES_KEY_LENGTH) {
            throw new IllegalArgumentException("AES key must be at least " + AES_KEY_LENGTH + " bytes");
        }
        byte[] key = new byte[AES_KEY_LENGTH];
        System.arraycopy(keyBytes, 0, key, 0, AES_KEY_LENGTH);
        return key;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * AES 加密
     *
     * @param plainText 明文
     * @return Base64 编码的密文
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);

            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("AES encryption failed", e);
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /**
     * AES 解密
     *
     * @param encryptedText Base64 编码的密文
     * @return 明文
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            if (combined.length < GCM_IV_LENGTH) {
                throw new RuntimeException("Invalid encrypted data: too short");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encryptedBytes = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            String result = new String(decryptedBytes, StandardCharsets.UTF_8);
            
            if (result.length() < 8) {
                log.warn("Decryption result seems truncated: length={}, content={}", result.length(), result);
            }
            
            return result;
        } catch (Exception e) {
            log.error("AES decryption failed: {}, encryptedText length: {}", e.getMessage(), encryptedText.length());
            throw new RuntimeException("AES decryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * 脱敏处理 API Key
     * 显示前4位 + 后4位，中间用星号替代
     *
     * @param apiKey 原始 API Key
     * @return 脱敏后的字符串
     */
    public String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}