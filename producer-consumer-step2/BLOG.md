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





저는 Springframework 에서 지원하는 [ThreadPoolTaskExecutor](https://docs.spring.io/spring/docs/4.1.x/javadoc-api/org/springframework/scheduling/concurrent/ThreadPoolTaskExecutor.html) 를 사용했습니다. 

ThreadPoolTaskExecutor 는 아래와 같은 속성을 가지고 있습니다.

```java
private int corePoolSize = 1; // 평상시 pool size
private int maxPoolSize = Integer.MAX_VALUE; 
private int keepAliveSeconds = 60; // core pool size에서 늘어나고 난 뒤 지속되는 시간
private int queueCapacity = Integer.MAX_VALUE; // queue 사이즈가 이만큼 차면 최대 max pool size까지 늘어난다
private boolean allowCoreThreadTimeOut = false; // true로 설정 하면 keepAliveSeconds에 의해 CoreThread도 제거된다.
```

ThreadPoolTaskExecutor는 ThreadPoolExecutor와 유사핮미나 JMX를 통해 런타임 모니터링을 수행하며 쓰레드풀을 관리하는기능을 가집니다.
Java docs를 보면 'This setting can be modified at runtime, for example through JMX.This setting can be modified at runtime, for example through JMX.' 문구를 심심치않게 볼 수 있습니다.




요구사항에서 이벤트 별 스레드 조건이 다를 수 있다고 했기 때문에 두 개의 ThreadPoolTaskExecutor 빈을 설정하겠습니다.

```java
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
```

당장은 스레드가 늘어나기 바라지 않기 때문에 setQueueCapacity를 주석처리 해주었습니다.(기본값은 정수 맥스)



이제 이 스레드 풀을 사용하도록 Consumer를 수정하겠습니다. 
제 첫번째 코드입니다. 이 코드에는 치명적인 결함이 있습니다.


```java
@Slf4j
public class BankConsumer<T extends PaymentEvent> {

    private final EventBroker<T> eventBroker;
    private final Consumer<T> consumer;

    public BankConsumer(EventBroker<T> eventBroker, Consumer<T> consumer, ThreadPoolTaskExecutor executor) {
        this.eventBroker = eventBroker;
        this.consumer = consumer;
        executor.execute(this::consume);
    }

    private void consume() {
        while (true) {
            try {
                T paymentEvent = this.eventBroker.poll();
                this.consumer.accept(paymentEvent);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
```





저는 스레드풀을 사용은 했지만 제대로 쓰진 않았습니다.

위 코드는 기존의 new Thread().start()를 ThreadPool.execute()로만 변경한 것인데, 조금만 살펴보면 스레드 풀에서 스레드 하나만 꺼내서 계속 while문만 돌리는 것을 알 수 있습니다. 즉, <u>요청이 많아져도 스레드풀의 이점을 전혀 살릴 수없는 코드</u>였습니다.





정말 바보같은 실수임을 깨닫고 다음처럼 변경했습니다.



```java
private final EventBroker<T> eventBroker;
    private final Consumer<T> consumer;
    private final ThreadPoolTaskExecutor executor;

    public BankConsumer(EventBroker<T> eventBroker, Consumer<T> consumer, ThreadPoolTaskExecutor executor) {
        this.eventBroker = eventBroker;
        this.consumer = consumer;
        this.executor = executor;
        new Thread(this::consume).start();
    }

    private void consume() {
        while (true) {
            try {
                T paymentEvent = this.eventBroker.poll();
                executor.execute(() -> consumer.accept(paymentEvent));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
```



하나의 스레드가 eventBroker에서 event를 꺼내고, ThreadPool에서 Thread를 꺼내 event를 넘겨주게 되었습니다. event를 poll하는 스레드가 프로세스가되고, 그 프로세스의 스레드가 된 것입니다.



![image](https://user-images.githubusercontent.com/47847993/80439415-70688900-8941-11ea-96a0-b3a8b6eb853f.png)



실행 후 로그를보면 카드결제는 2개의 스레드(entThreadPool-2, 1) 현금결제는 1개의 스레드를 사용하는 것을 볼 수 있습니다. 설정 시 현금결제는 coreThreadPool을 1로 뒀기 때문입니다.



리뷰때 이야기를 했던 내용 중, EventBroker가 동기화되지 않는 Queue를 가지고 있어도 되는지에 대한 주제가 있었습니다. 현재 코드에서 EventBroker는 아래와 같은 LinkedList 큐를 가지고 있습니다.

```java
private final Queue<T> eventQueue = new LinkedList<>();
```










https://www.baeldung.com/java-threadpooltaskexecutor-core-vs-max-poolsize
