package com.nutrition;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EnvTest {
    @Value("${spring.datasource.password}")
    private String dbPwd;

    @Test
    void printPwd(){
        System.out.println("读取到的数据库密码："+dbPwd);
    }
}