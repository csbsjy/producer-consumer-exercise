package com.javabom.producercomsumer.producercomsumer.event;

import com.javabom.producercomsumer.producercomsumer.dto.CashPaymentRequestDto;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class CashPayEvent implements PaymentEvent {

    private static final String EVENT_NAME = "현금결제이벤트";

    private final CashPaymentRequestDto cashPaymentRequestDto;

    public CashPayEvent(CashPaymentRequestDto cashPaymentRequestDto) {
        this.cashPaymentRequestDto = cashPaymentRequestDto;
    }

    @Override
    public void run() {
        log.info("{}: CashPaymentEvent를 소모합니다 ... ", Thread.currentThread().getThreadGroup().getName());
    }

    @Override
    public String getName() {
        return EVENT_NAME;
    }

    @Override
    public String toString() {
        return "CashPaymentEvent{" +
                "cashPaymentRequestDto=" + cashPaymentRequestDto +
                '}';
    }
}
