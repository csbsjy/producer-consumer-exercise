## producer-consumer 1단계



이펙티브자바에서 제네릭파트를 읽고 제네릭은 직접 써보지 않으면 익숙해기 힘들겠다 라는 생각에 진행했던 과제(@copyright crud)에 대해 작성해보려고 합니다. 





프로젝트 이름은 "Producer - Consumer" 이며 다음 Github에서 자바봄 멤버들의 기록을 볼 수 있습니다.(https://github.com/Java-Bom/producer-consumer)



큰 주제는 간단한 비동기 이벤트 프로그래밍입니다. 이벤트를 생성하고 소모하는 프로그램을 만드는데 한 번 "제네릭"을 신경써서 써보자 라는 목표가 였습니다. (뒤로 갈수록 스레드의 늪에 빠졌지만 ㅎㅅㅎ)





1단계 full code는 [여기](https://github.com/csbsjy/producer-consumer-exercise)에서 볼 수 있습니다





**<u>1단계 미션</u>**은 다음과 같았습니다.

```
1. 사용자가 결제를 요청할 수 있다.
   - 결제의 종류는 카드결제와 현금결제 두가지다.
   - 카드결제 이벤트는 카드사 이름과 결제금액만 받아서 결제한다.
   - 현금결제는 결제금액, 이름을 받아 결제한다.

2. 받은 결제 요청을 비동기로 처리한다.
    - 카드결제와 현금결제는 각각 스레드 1개씩으로 처리한다.
    - 이벤트를 소모하기 시작할때 로그로 남겨놓는다.
    - 결제 요청이 100개 이상 쌓여있는 상태에서 들어오는 요청은 실패처리한다.

3. 결제가 성공하면 디비에 저장한다.
    - 카드결제이력과 현금결제이력을 따로 관리한다.
```



1번 요구사항은 비교적 간단합니다. 



<u>요구사항 1. 사용자가 결제를 요청할 수 있다.</u>

- 결제의 종류는 카드결제와 현금결제 두가지다.
- 카드결제 이벤트는 카드사 이름과 결제금액만 받아서 결제한다.
- 현금결제는 결제금액, 이름을 받아 결제한다.



Client 인터페이스에 대한 요구사항이라는 것을 쉽게 파악할 수 있습니다. 이 내용은 Controller단에 녹일 수 있겠습니다.



저는 `/card` 와 `/cash` 로 요청을 받고, 각각의 Input 요구사항은 별개의 Dto를 만듦으로써 수현하였습니다.



코드는 아래와 같습니다.(편의상 import 문은 생략하였습니다.)



**PayController.java**

```java
@RestController
@RequiredArgsConstructor
@Slf4j
public class PayController {

    private final CashPaymentService cashPaymentService;
    private final CardPaymentService cardPaymentService;

    @PostMapping("/cash")
    public ResponseEntity<String> pay(@RequestBody CashPaymentRequestDto cashPaymentRequestDto) {
        cashPaymentService.requestPay(cashPaymentRequestDto);
        return ResponseEntity.ok("정상적으로 지불되었습니다");
    }

    @PostMapping(value = "/card")
    public ResponseEntity<String> charge(@RequestBody CardPaymentRequestDto cardPaymentRequestDto) {
        cardPaymentService.requestPay(cardPaymentRequestDto);
        return ResponseEntity.ok("정상적으로 지불되었습니다");
    }

}
```



**CardPaymentRequestDto.java**

```java
@Getter
@NoArgsConstructor
public class CardPaymentRequestDto {
    private String userId;
    private int price; // 금액
    private String cardCompany; // 카드사 이름

    public CardPaymentRequestDto(String userId, int price, String cardCompany) {
        this.userId = userId;
        this.price = price;
        this.cardCompany = cardCompany;
    }

    @Override
    public String toString() {
        return "CardPaymentRequestDto{" +
                "userId='" + userId + '\'' +
                ", price=" + price +
                ", cardCompany='" + cardCompany + '\'' +
                '}';
    }
}
```



**CashPaymentRequestDto.java**

```java
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashPaymentRequestDto {
    private String userId;
    private String productName; // 상품이름
    private int price; // 금액

    @Builder
    public CashPaymentRequestDto(String userId, String productName, int price) {
        this.userId = userId;
        this.productName = productName;
        this.price = price;
    }

    @Override
    public String toString() {
        return "CashPaymentRequestDto{" +
                "userId='" + userId + '\'' +
                ", productName='" + productName + '\'' +
                ", price=" + price +
                '}';
    }
}
```





그리고 요청이 들어온 이벤트를 넣고 빼줄 **<u>EventBroker</u>** 클래스가 필요합니다. Event는 Queue에 저장되며, 저는 단순 LinkedList에 넣는 것을 택했습니다. (스레드 1개씩 밖에 없기 때문에 동기화 이슈는 발생하지 않을 것 입니다.)



그리고 드디어 첫 제네릭이 등장합니다(?)



**EventBroker.java**

```java
@Slf4j
public class EventBroker<T extends PaymentEvent> {
    private Queue<T> eventQueue = new LinkedList<>();

    public void offer(T payEvent) { 
        eventQueue.offer(payEvent);
    }

    public T poll() throws InterruptedException {
        while (eventQueue.size() <= 0) { // Queue에 이벤트가 쌓일 때까지 대기
            Thread.sleep(3000);
        }
        return eventQueue.poll();
    }
}

```



EventBroker는 내부에 이벤트를 순서대로 적재하고 순서대로 꺼내줄 Queue를 가지고 있습니다. PayEvent는 클라이언트로부터 오는 요청을 의미할 것이고, 여기에서는 "현금결제 이벤트", "카드결제 이벤트" 두 종류이기 때문에 PayEvent라는 이벤트로 일반화하고 <u>"한정적 제네릭타입"</u>을 활용하였습니다. 



코드가 있지는 않지만 PayEvent 는 단순 로그를 찍어줄 목적인 run() 메서드만 존재합니다. 





2번 요구사항부터 멘붕이었습니다. 고백하자면 저는 비동기와 스레드 키워드 공포증이 있습니다. 이 과제를 계기로 극복해나가기를 기대합니다. 





**<u>요구사항 2. 받은 결제 요청을 비동기로 처리한다.</u>**

- 카드결제와 현금결제는 각각 스레드 1개씩으로 처리한다.
- 이벤트를 소모하기 시작할때 로그로 남겨놓는다.
- 결제 요청이 100개 이상 쌓여있는 상태에서 들어오는 요청은 실패처리한다.



우선, 요청을 받고 EventBroker에 넘겨주는 로직을 Service에 담았습니다.(CardPayService, CashPayService가 존재하지만 전체적인 로직은 동일하기 때문에 하나만 작성하도록 하겠습니다.)



**CardPaymentService.java**

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CardPaymentService {

    private final AccountRepository accountRepository;

    public void requestPay(final CardPaymentRequestDto cardPaymentRequestDto) {
        
//EventBrokerGroup에서 EventBroker를 찾고 Event를 Queue에 추가하는 요청을 한다
      EventBrokerGroup.findByPayEvent(CardPayEvent.class).offer(new CardPayEvent(cardPaymentRequestDto)); 
    }

  
  // Event를 소모하는 메서드 ==> "실제 결제 수행"
    @Transactional
    public void pay(final CardPayEvent paymentEvent) {
        paymentEvent.run(); // log 용 - "2) event를 소모하기 시작할 때 로그를 남겨놓는다"
        Account account = accountRepository.findAccountByUserId(paymentEvent.getCardPaymentRequestDto().getUserId())
                .orElseThrow(IllegalArgumentException::new);
        account.cardPay(paymentEvent.getCardPaymentRequestDto());
    }

}

```





requestPay 메시지는 Controller에서 Client의 요청이 들어왔을 때 불리는 메시지입니다. EventBroker에 Event를 넘겨주는 역할을 합니다. EventBroker 역시 카드, 현금별로 존재하게 되는데 Enum 에도 익숙해지고자 한번 사용해보았습니다.





넘겨주는 PayEvent 타입에 따라 EventBroker를 넘겨주는 Enum 클래스입니다. 기존에는 Configuration 에 EventBroker 빈을 Event별로 띄워주어 Event가 늘어날수록 Configuration 의 길이가 함께 길어지는 구조였습니다. EventBroker 생성에 대한 책임을 EventBrokerGroup 으로 위임하니 한결 간단해졌습니다. 이 구조로 바꾸니 새로운 이벤트 생성 시 상수만 추가해주면 다른 코드를 건드릴 필요가 없어졌습니다.



**EventBrokerGroup.java**

```java
public enum EventBrokerGroup {
    CARD(CardPayEvent.class, new EventBroker<CardPayEvent>()),
    CASH(CashPayEvent.class, new EventBroker<CashPayEvent>());

    private Class<? extends PaymentEvent> payEvent;
    private EventBroker<? extends PaymentEvent> payEventBroker;

    EventBrokerGroup(Class<? extends PaymentEvent> payEvent, EventBroker<? extends PaymentEvent> payEventBroker) {
        this.payEvent = payEvent;
        this.payEventBroker = payEventBroker;
    }

    @SuppressWarnings("unchecked")
    public static <T extends PaymentEvent> EventBroker<T> findByPayEvent(Class<T> payEvent) {
        return (EventBroker<T>) Arrays.stream(EventBrokerGroup.values())
                .filter(event -> event.getPayEvent().equals(payEvent))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당하는 EventBroker가 없습니다"))
                .getPayEventBroker();
    }

    private EventBroker<?> getPayEventBroker() {
        return payEventBroker;
    }

    public Class<? extends PaymentEvent> getPayEvent() {
        return payEvent;
    }
}
```







Event를 넣어줬으니 이제 소모하는 Consumer가 있어야합니다. 솔직히 저는 Consumer가 뭔지도 잘 몰랐어서 감을 못잡았었는데, 출제자가 어느정도 가닥을 잡아줬습니다. 스레드 1개씩이니 각 이벤트별 스레드를 소모하는 Consumer를 만들면 되겠구나 싶었습니다. 





아래는 BankConsumer 코드입니다.

 

BankConsumer는 큐를 꺼내서 실행하는 역할을 수행합니다. 따라서 멤버변수는 EventBroker와 Consumer인데, 여기서 Consumer는 인자(PayEvent)를 받아서 이벤트(결제)를 수행해줄 void 타입의 메서드를 의미합니다.

즉, 위에서 작성한 Service 의 pay 메서드에 해당합니다.



**BanckConsumer.java**

```java
@Slf4j
public class BankConsumer<T extends PaymentEvent> {

    private final EventBroker<T> eventBroker;
    private final Consumer<T> consumer;

  // BankConsumer가 생성됨과 동시에 Thread 한개를 생성하여 이벤트를 소모하도록 한다. 
    public BankConsumer(EventBroker<T> eventBroker, Consumer<T> consumer) {
        this.eventBroker = eventBroker;
        this.consumer = consumer;
        new Thread(this::consume).start();
    }

    private void consume() {
      // 하나의 스레드는 계속해서 EventBroker에서 Event를 꺼내고 소모한다.
        while (true) {
            try {
                T paymentEvent = this.eventBroker.poll();
                Thread.currentThread().setName(paymentEvent.getName());
                this.consumer.accept(paymentEvent);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

```





그리고 Consumer를 PayEvent 타입별로 생성해줄 Config 는 다음과 같습니다.



**DefaultConfig.java**

```java
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

```







아직 2번 요구사항의 "결제 요청이 100개 이상 쌓여있는 상태에서 들어오는 요청은 실패처리한다."를 구현하지 않았습니다. EventBroker 에 다음과 같은 Validation 을 추가해줍니다.





```java
private static final int LIMIT_QUEUE_SIZE = 100;
private Queue<T> eventQueue = new LinkedList<>();

public void offer(T payEvent) {
    if (eventQueue.size() > LIMIT_QUEUE_SIZE) {
        throw new IllegalArgumentException("더이상 요청할 수 없습니다");
    }
    log.info("offer {} Event in EventBroker", payEvent.getName());
    eventQueue.offer(payEvent);
}
```

 







이제 마지막 요구사항만 남았습니다.

<u>**결제가 성공하면 디비에 저장한다.**</u>

- 카드결제이력과 현금결제이력을 따로 관리한다.







Spring-data-jpa 와 h2를 사용했습니다. JPA 실습은 아니기 때문에 간단히 설명만 하고 넘어가면, 

- Account Entity는 클라이언트의 계좌를 의미합니다. userId 를 가지고있고, 현금결제 이력과 카드결제 이력과 OneToMany 연관관계를 가집니다. 
- 현금결제이력(CashPaymentRecordEntity), 카드결제이력(CardPaymentRecordEntity)는 Entity 로 승격했고, 각각 Dto를 통해 받은 상품의 이름, 금액정보를 가지고 있습니다. 별도의 Table 로 관리됩니다.







정상적으로 수행되는지 확인하기 위해 다음의 http test를 작성해보았습니다.

**threadtest.http**

```http
POST http://localhost:8888/cash
Content-Type: application/json

{
  "userId": "seojaeyeon",
  "productName": "감자",
  "price": 12000
}

###

POST http://localhost:8888/card
Content-Type: application/json

{
  "userId": "seojaeyeon",
  "cardCompany": "신한",
  "price": 12000
}

###
POST http://localhost:8888/cash
Content-Type: application/json

{
  "userId": "seojaeyeon",
  "productName": "고구마",
  "price": 10000
}

###

POST http://localhost:8888/card
Content-Type: application/json

{
  "userId": "seojaeyeon",
  "cardCompany": "우리",
  "price": 17000
}

###
POST http://localhost:8888/cash
Content-Type: application/json

{
  "userId": "seojaeyeon",
  "productName": "토마토",
  "price": 7000
}

###

POST http://localhost:8888/card
Content-Type: application/json

{
  "userId": "seojaeyeon",
  "cardCompany": "국민",
  "price": 56000
}

###

```





애플리케이션을 실행한 뒤 위의 요청을 실행하고 로그를 하나씩 살펴보겠습니다.



1. 톰캣스레드에 의해 현금결제이벤트와 카드결제이벤트가 offer 되었습니다. 

![image](https://user-images.githubusercontent.com/47847993/79101738-2b225400-7da4-11ea-822f-15861c96ffd6.png)



2. [] 안의 값이 스레드의 이름입니다. Consumer 에서 Thread의 이름을 지정해주었고, 그에따라 아래와 같이 출력되는 것을 볼 수 있습니다. 현금결제이벤트 스레드와 카드결제이벤트스레드가 이벤트를 소모합니다. 

![image](https://user-images.githubusercontent.com/47847993/79101789-47be8c00-7da4-11ea-89f3-ff080ab5d3bb.png)

![image](https://user-images.githubusercontent.com/47847993/79101869-7e94a200-7da4-11ea-8fd7-cb075f4bd078.png)



그리고 위의 과정을 총 3번 반복합니다.(현금결제요청 3번, 카드결제요청 3번) 스레드는 1개로 수행되고 있으며(요구사항 2번 만족), 그에따라 동기화 문제도 발생하지 않습니다.이 상황에서 하나의 이벤트 큐는 하나의 스레드만 접근하기 때문입니다.





모식도를 그려보면 아래와 같습니다. 



![image](https://user-images.githubusercontent.com/47847993/79101970-b996d580-7da4-11ea-8c9c-66766e1a350f.png)

![image](https://user-images.githubusercontent.com/47847993/79101989-c6b3c480-7da4-11ea-9d80-ae6a722f5b0c.png)





그리고 DB에도 정상적으로 데이터 삽입이 이루어집니다.

![image](https://user-images.githubusercontent.com/47847993/79102146-1c886c80-7da5-11ea-8f11-54f97cabedf1.png)







1단계 과제를 마쳤습니다 ! 와 ! 나도 이제 이벤트프로그래밍한다! 라고 말할 수 있을 줄 알았는데 오산이었습니다. 



다음 단계를 스포하자면, 

```
1. 각각 받은 요청은 실패할 수 있다.
      - 실패가 일어나면 다시 큐에 넣어준다.
      - 현금결제는 2번 카드결제는 3번까지 재시도 후 실패하면 그때 디비에 실패상태를 기록한다.
      
2. 카드결제와 현금결제의 스레드는 언제든지 추가될 수 있다.
      - 현재 카드결제와 현금결제는 각각 1개의 스레드로 운영된다.
      - 하지만 다시 어플리케이션을 돌릴때 카드결제는 2개의 스레드 현금결제는 1개의 스레드로 운영될 수 있는 환경이어야한다.

3. 컨슈머에 대한 **테스트 코드**는 필수
```





여기까지 작성한 코드는 2번을 적용할 때 아주 고칠 부분이 많아졌습니다. 1개의 스레드일 때 고려하지 않았던 부분이 멀티스레드로 구조를 바꾸니 자칫하면 이벤트 유실이 일어날 수 있는 프로그램이 되었습니다.



다음 포스팅은 2단계 요구사항을 해결해보겠습니다.

