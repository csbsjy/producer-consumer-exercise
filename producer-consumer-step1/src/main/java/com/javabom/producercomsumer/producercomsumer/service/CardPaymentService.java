package com.javabom.producercomsumer.producercomsumer.service;

import com.javabom.producercomsumer.producercomsumer.domain.Account;
import com.javabom.producercomsumer.producercomsumer.domain.AccountRepository;
import com.javabom.producercomsumer.producercomsumer.dto.CardPaymentRequestDto;
import com.javabom.producercomsumer.producercomsumer.event.CardPayEvent;
import com.javabom.producercomsumer.producercomsumer.event.EventBrokerGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardPaymentService {

    private final AccountRepository accountRepository;

    public void requestPay(final CardPaymentRequestDto cardPaymentRequestDto) {
        EventBrokerGroup.findByPayEvent(CardPayEvent.class).offer(new CardPayEvent(cardPaymentRequestDto));
    }

    @Transactional
    public void pay(final CardPayEvent paymentEvent) {
        paymentEvent.run();
        Account account = accountRepository.findAccountByUserId(paymentEvent.getCardPaymentRequestDto().getUserId())
                .orElseThrow(IllegalArgumentException::new);
        account.cardPay(paymentEvent.getCardPaymentRequestDto());
    }

}
