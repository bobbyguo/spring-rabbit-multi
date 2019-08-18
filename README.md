# spring-rabbit-multi

## How-to
To use the library, the project must:
1. Import the dependency to **spring-rabbit-multi**;
2. Provide the map of configuration for multi rabbit connections with prefix **spring.rabbit.multi**. All 
   attributes available for **spring.rabbitmq** can be used in **spring.rabbit.multi**. 
3. Change the Rabbit context when talking to different servers:
   1. For ```RabbitTemplate```, use ```SimpleResourceHolder.bind()``` and ```SimpleResourceHolder.unbind()```;
   2. For ```@RabbitListener```, define the ```containerFactory``` or leave it blank for the default connection.

##### 1. Main SpringBoot class
```java
@EnableRabbit
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

##### 2. pom.xml
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>com.github.bobbyguo</groupId>
    <artifactId>spring-rabbit-multi</artifactId>
    <version>${version}</version>
</dependency>
```

##### 3. application.yml
```yaml
spring:
    rabbitmq:
        host: localhost
        port: 5672
    multi:
        rabbit1:
            host: 172.16.2.122
            port: 5672
        rabbit2:
            host: 172.16.2.150
            port: 5672
```

##### 4.1. Change context when using RabbitTemplate
```java
@Autowired
private RabbitTemplate rabbitTemplate;

void someMethod() {
    SimpleResourceHolder.bind(rabbitTemplate.getConnectionFactory(), "rabbit1");
    rabbitTemplate.convertAndSend("someExchange", "someRoutingKey", "someMessage");
    SimpleResourceHolder.unbind(rabbitTemplate.getConnectionFactory());
}
```

##### 4.2. Change context on RabbitListener
```java
/**
 * Listener for the default connection. 
 */
@RabbitListener(queues = "queueName")
void someListener(String message) {
    // Consume message
}

/**
 * Listener for the server named as `rabbit1`. 
 */
@RabbitListener(queues = "queueOfRabbit1", containerFactory = "rabbit1") 
void anotherListener(String message) {
    // Consume message
}
```

## Configuration
This library enables the possibility of having multiple Rabbit servers automatically, configured from the property
**spring.rabbit.multi**. However, for maximum compatibility, it does not change the default capacity of configuring a
connection with the existent **spring.rabbitmq** property.

Thus, it's important to understand how the application will behave when multiple configurations are provided:
* Unlimited number of connections can be set, but the user must be aware of the implications of maintaining many 
connections;
* The default connection **spring.rabbitmq** must exist, and the configuration under **spring.rabbit.multi** will share the same
properties under **spring.rabbitmq** unless they are not replaced.
* For convention: the **key** under **spring.rabbit.multi** is used to be the name of rabbit **connectionName(using in rabbitTemplate)** and **containerName(using in rabbit listener)**.
