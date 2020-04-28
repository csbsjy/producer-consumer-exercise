package com.javabom.producercomsumer.producercomsumer.config;

import com.javabom.producercomsumer.producercomsumer.consumer.BankConsumer;
import com.javabom.producercomsumer.producercomsumer.event.CardPayEvent;
import com.javabom.producercomsumer.producercomsumer.event.CashPayEvent;
import com.javabom.producercomsumer.producercomsumer.event.EventBrokerGroup;
import com.javabom.producercomsumer.producercomsumer.service.CardPaymentService;
import com.javabom.producercomsumer.producercomsumer.service.CashPaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class DefaultConfig {

    @Bean
    public BankConsumer<CardPayEvent> cardEventConsumer(CardPaymentService cardPaymentService, ThreadPoolTaskExecutor cardEventThreadPool) {
        return new BankConsumer<>(EventBrokerGroup.findByPayEvent(CardPayEvent.class), cardPaymentService::pay, cardEventThreadPool);
    }

    @Bean
    public BankConsumer<CashPayEvent> cashEventConsumer(CashPaymentService cashPaymentService, ThreadPoolTaskExecutor cashEventThreadPool) {
        return new BankConsumer<>(EventBrokerGroup.findByPayEvent(CashPayEvent.class), cashPaymentService::pay, cashEventThreadPool);
    }


}
