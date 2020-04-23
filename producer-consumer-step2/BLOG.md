## producer-consumer 2단계



1단계 코드에서 2단계 요구사항을 적용하고나서 피드백 폭탄을 받았습니다. 멀티스레드 환경, 동기화, 그리고 동시성문제는 제 예상보다 훨씬 더 고려해야할 사항이 많다는 생각이 들었고 많은 연습이 필요할 것 같습니다.





**<u>2단계 미션</u>**은 다음과 같습니다.

```java
1. 각각 받은 요청은 실패할 수 있다.      
  - 실패가 일어나면 다시 큐에 넣어준다.      
  - 현금결제는 2번 카드결제는 3번까지 재시도 후 실패하면 그때 디비에 실패상태를 기록한다.      
2. 카드결제와 현금결제의 스레드는 언제든지 추가될 수 있다.      
  - 현재 카드결제와 현금결제는 각각 1개의 스레드로 운영된다.      
  - 하지만 다시 어플리케이션을 돌릴때 카드결제는 2개의 스레드 현금결제는 1개의 스레드로 운영될 수 있는 환경이어야한다.
3. 컨슈머에 대한 **테스트 코드**는 필수
```





2번 요구사항 먼저 적용하겠습니다



<u>2.카드결제와 현금결제의 스레드는 언제든지 추가될 수 있다.</u>

현재 Consumer 코드는 아래와 같습니다. 

1단계 요구사항대로 Thread 를 하나 생성하고 카드이벤트, 현금이벤트를 스레드 하나가 각각 소모하고 있습니다. 



```java
public BankConsumer(EventBroker<T> eventBroker, Consumer<T> consumer) {
        this.eventBroker = eventBroker;
        this.consumer = consumer;
        new Thread(this::consume).start(); // Thread 를 하나 만든다
    }

    private void consume() {
        while (true) { // 하나의 스레드가 계속 while 문을 돌며 Event 를 꺼낸 뒤 소모한다. 
            try {
                T paymentEvent = this.eventBroker.poll();
                Thread.currentThread().setName(paymentEvent.getName());
                this.consumer.accept(paymentEvent);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
```





언제든지 스레드를 추가할 수 있게 하기 위해서 스레드를 담아두고 꺼내서 쓸 수 있어야 한다고 생각했습니다. "ThreadPool" 개념이 등장합니다.

> ThreadPool 
>
> 스레드가 필요할 때마다 생성하여 사용하면 성능상 좋지 않기 때문에 Pool 에 스레드를 미리 생성해두고 필요할 때마다 꺼내쓸 수 있도록 한다.





저는 Springframework 에서 지원하는 ThreadPoolTaskExecutor 를 사용했습니다. 

ThreadPoolTaskExecutor 는 아래와 같은 속성을 가지고 있습니다.

```java
private int corePoolSize = 1;
private int maxPoolSize = Integer.MAX_VALUE;
private int keepAliveSeconds = 60;
private int queueCapacity = Integer.MAX_VALUE;
private boolean allowCoreThreadTimeOut = false;
```





이벤트 별 스레드 조건이 다르기 때문에 두 개의 ThreadPoolTaskExecutor 빈을 설정합니다.





https://www.baeldung.com/java-threadpooltaskexecutor-core-vs-max-poolsize
