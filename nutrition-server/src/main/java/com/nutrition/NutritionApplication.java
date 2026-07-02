package com.nutrition;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 营养助手 - 后端启动类
 */
@SpringBootApplication
@MapperScan("com.nutrition.mapper")
public class NutritionApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutritionApplication.class, args);
        System.out.println("======================================");
        System.out.println("  营养助手服务端启动成功！");
        System.out.println("  访问地址: http://localhost:8088");
        System.out.println("======================================");
    }
}
