package com.javabom.producercomsumer.producercomsumer.event;

import com.javabom.producercomsumer.producercomsumer.dto.CardPaymentRequestDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class CardPayEvent implements PaymentEvent {

    private static final String EVENT_NAME = "카드결제이벤트";

    private final CardPaymentRequestDto cardPaymentRequestDto;

    public CardPayEvent(CardPaymentRequestDto cardPaymentRequestDto) {
        this.cardPaymentRequestDto = cardPaymentRequestDto;
    }

    @Override
    public void run() {
        log.info("{}: CardPaymentEvent를 소모합니다 ... ", Thread.currentThread().getThreadGroup().getName());
    }

    @Override
    public String getName() {
        return EVENT_NAME;
    }

    @Override
    public String toString() {
        return "CardPaymentEvent{" +
                "cardPaymentRequestDto=" + cardPaymentRequestDto +
                '}';
    }
}

