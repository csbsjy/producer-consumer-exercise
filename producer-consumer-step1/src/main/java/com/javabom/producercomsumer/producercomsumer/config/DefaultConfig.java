package com.javabom.producercomsumer.producercomsumer.config;

import com.javabom.producercomsumer.producercomsumer.consumer.BankConsumer;
import com.javabom.producercomsumer.producercomsumer.event.CardPayEvent;
import com.javabom.producercomsumer.producercomsumer.event.CashPayEvent;
import com.javabom.producercomsumer.producercomsumer.event.EventBrokerGroup;
import com.javabom.producercomsumer.producercomsumer.service.CardPaymentService;
import com.javabom.producercomsumer.producercomsumer.service.CashPaymentService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultConfig {

    @Bean
    public BankConsumer<CardPayEvent> chargeEventConsumer(CardPaymentService cardPaymentService) {
        return new BankConsumer<>(EventBrokerGroup.findByPayEvent(CardPayEvent.class), cardPaymentService::pay);
    }

    @Bean
    public BankConsumer<CashPayEvent> payEventConsumer(CashPaymentService cashPaymentService) {
        return new BankConsumer<>(EventBrokerGroup.findByPayEvent(CashPayEvent.class), cashPaymentService::pay);
    }
}
