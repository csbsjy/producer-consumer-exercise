package com.javabom.producercomsumer.producercomsumer.event;


import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.Queue;

@Slf4j
public class EventBroker<T extends PaymentEvent> {
    private static final int LIMIT_QUEUE_SIZE = 100;
    private Queue<T> eventQueue = new LinkedList<>();

    public void offer(T payEvent) {
        if (eventQueue.size() > LIMIT_QUEUE_SIZE) {
            throw new IllegalArgumentException("더이상 요청할 수 없습니다");
        }
        log.info("offer {} Event in EventBroker", payEvent.getName());
        eventQueue.offer(payEvent);
    }

    public T poll() throws InterruptedException {
        while (eventQueue.size() <= 0) {
            Thread.sleep(3000);
        }
        return eventQueue.poll();
    }
}
