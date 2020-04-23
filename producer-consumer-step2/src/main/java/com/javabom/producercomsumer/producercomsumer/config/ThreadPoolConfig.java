package com.javabom.producercomsumer.producercomsumer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolConfig {
    @Bean
    public ThreadPoolTaskExecutor cardEventThreadPool() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(5); // corePoolSize is the minimum number of workers to keep alive
        taskExecutor.setMaxPoolSize(10); // maxPoolSize defines the maximum number of threads that can ever be created
        taskExecutor.setQueueCapacity(3); // maxPoolSize depends on queueCapacity
        taskExecutor.setThreadGroupName("카드결제이벤트그룹");
        return taskExecutor;
    }

}
