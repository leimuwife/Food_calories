package com.nutrition;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtSecretGen {
    public static void main(String[] args) {
        // 生成256位安全随机密钥（JWT推荐长度）
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String secret = Base64.getEncoder().encodeToString(bytes);
        System.out.println("你的JWT密钥：" + secret);
    }
}