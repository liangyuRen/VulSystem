package com.nju.backend.config;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "projectAnalysisExecutor")
    public Executor projectAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 设置核心线程数
        executor.setMaxPoolSize(20);  // 设置最大线程数
        executor.setQueueCapacity(100); // 设置队列容量
        executor.setThreadNamePrefix("project-analysis-");
        executor.initialize();
        return executor;
    }

    @Bean
    public RestTemplate restTemplate() {
        // 创建HttpClient配置超时
        HttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(100)           // 最大连接数
                .setMaxConnPerRoute(10)         // 每个路由最大连接数
                .build();

        // 创建带超时的HttpComponentsClientHttpRequestFactory
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        // 设置超时时间（毫秒）
        factory.setConnectTimeout(10000);       // 连接超时: 10秒
        factory.setReadTimeout(30000);          // 读取超时: 30秒
        factory.setConnectionRequestTimeout(5000); // 连接请求超时: 5秒

        return new RestTemplate(factory);
    }
}

