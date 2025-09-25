package com.nju.backend.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XxlJobConfig {

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses("http://localhost:8080/xxl-job-admin");
        xxlJobSpringExecutor.setAppname("springboot-job");
        xxlJobSpringExecutor.setAccessToken("default_token");
        xxlJobSpringExecutor.setIp("127.0.0.1");
        xxlJobSpringExecutor.setPort(9999);
        xxlJobSpringExecutor.setLogPath("./data/applogs/xxl-job/jobhandler");
//        xxl.job.executor.logpath=./logs/xxl-job/jobhandler

//        xxlJobSpringExecutor.setLogPath("/Users/mac/applogs/xxl-job/jobhandler");
        xxlJobSpringExecutor.setLogRetentionDays(30);
        return xxlJobSpringExecutor;
    }
}
