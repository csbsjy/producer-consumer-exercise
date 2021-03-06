package com.javabom.producercomsumer.producercomsumer.domain;

import com.javabom.producercomsumer.producercomsumer.dto.CardPaymentRequestDto;
import com.javabom.producercomsumer.producercomsumer.dto.CashPaymentRequestDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Entity
@Getter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID")
    private List<CardPaymentRecordEntity> cardPaymentHistories = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID")
    private List<CashPaymentRecordEntity> cashPaymentHistories = new ArrayList<>();

    public Account(String userId) {
        this.userId = userId;
    }

    public void cardPay(CardPaymentRequestDto cardPaymentRequestDto) {
        log.info("CardPay: {}, {}", cardPaymentRequestDto.getCardCompany(), cardPaymentRequestDto.getPrice());
        this.cardPaymentHistories.add(new CardPaymentRecordEntity(cardPaymentRequestDto));
    }

    public void cashPay(CashPaymentRequestDto cashPaymentRequestDto) {
        log.info("CashPay: {}, {}", cashPaymentRequestDto.getProductName(), cashPaymentRequestDto.getPrice());
        this.cashPaymentHistories.add(new CashPaymentRecordEntity(cashPaymentRequestDto));
    }

    public void clearCardPayHistories() {
        cardPaymentHistories.clear();
    }

    public void clearCashPayHistories() {
        cashPaymentHistories.clear();
    }
}
