package com.javabom.producercomsumer.producercomsumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolTaskExecutor cardEventThreadPool() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
//        taskExecutor.setQueueCapacity(1);
        taskExecutor.setThreadGroupName("카드결제이벤트그룹");
        return taskExecutor;
    }

    @Bean
    public ThreadPoolTaskExecutor cashEventThreadPool() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
//        taskExecutor.setQueueCapacity(1);
        taskExecutor.setThreadGroupName("현금결제이벤트그룹");
        return taskExecutor;
    }

}
