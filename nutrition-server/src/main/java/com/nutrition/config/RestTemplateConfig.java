package com.nutrition.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class RestTemplateConfig {

    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int READ_TIMEOUT_SECONDS = 60;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                .setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                .build();
    }

    @Bean("aiRestTemplate")
    public RestTemplate aiRestTemplate() throws Exception {
        // 1. 仅启用 TLS 1.2 / 1.3，兼容阿里云接口要求
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, null);
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.2", "TLSv1.3"},
                null,
                null
        );

        // 2. 注册HTTP/HTTPS套接字工厂
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        // 3. 配置连接池
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(20);

        // 4. 配置超时（全部通过HttpClient原生配置实现）
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setResponseTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .setConnectionRequestTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        // 5. 构建HttpClient
        var httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        // 6. 包装为Spring的请求工厂
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getMessageConverters().add(0,
                new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}