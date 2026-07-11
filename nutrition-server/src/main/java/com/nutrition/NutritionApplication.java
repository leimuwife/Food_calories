package com.nutrition;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 营养助手 - 后端启动类
 */
@SpringBootApplication
@MapperScan("com.nutrition.mapper")
@EnableCaching
@EnableScheduling
public class NutritionApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutritionApplication.class, args);
        System.out.println("======================================");
        System.out.println("  营养助手服务端启动成功！");
        System.out.println("======================================");
    }
}
