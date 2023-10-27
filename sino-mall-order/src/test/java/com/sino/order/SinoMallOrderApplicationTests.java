package com.sino.order;

import com.sino.order.entity.OrderEntity;
import com.sino.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@Slf4j
@SpringBootTest
class SinoMallOrderApplicationTests {

	@Autowired
	private AmqpAdmin amqpAdmin;

	@Autowired
	private RabbitTemplate template;

	@Test
	void testSendMessage() {

		for (int i = 0; i <10; i++) {
			if (i%2==0){
				OrderReturnReasonEntity returnReasonEntity = new OrderReturnReasonEntity();
				returnReasonEntity.setId(1L);
				returnReasonEntity.setName("张三");
				returnReasonEntity.setStatus(1);
				// 发送消息如果是一个对象会使用jdk自带的序列化机制， 如果想使用json序列化，需要在容器中放入一个jackson的对象
				template.convertAndSend("hell.java.directExchange", "hell.java",returnReasonEntity,new CorrelationData(UUID.randomUUID().toString()));
			}else {
				OrderEntity orderEntity = new OrderEntity();
				orderEntity.setOrderSn(UUID.randomUUID().toString());
				template.convertAndSend("hell.java.directExchange", "hell.java1",orderEntity,new CorrelationData(UUID.randomUUID().toString()));
			}

		}
	}

	@Test
	void createExchange() {
		//DirectExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
		DirectExchange directExchange = new DirectExchange("hell.java.directExchange",true, false);
		amqpAdmin.declareExchange(directExchange);
		log.info("交换机【{}】创建成功", "hell.java.directExchange");
	}

	@Test
	void createQueue() {
		//Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, @Nullable Map<String, Object> arguments) {
		Queue queue = new Queue("hello.java.queue", true,false,false);
		amqpAdmin.declareQueue(queue);
		log.info("队列【{}】创建成功", "hell.java.queue");
	}

	@Test
	void createBinding() {
		//Binding(String destination[目的地，队列或交换机名], Binding.DestinationType destinationType【目的地类型】, String exchange, String routingKey, @Nullable Map<String, Object> arguments) {
		Binding binding = new Binding("hello.java.queue", Binding.DestinationType.QUEUE, "hell.java.directExchange", "hell.java",null);
		amqpAdmin.declareBinding(binding);
		log.info("绑定【{}】创建成功", "hell.java");
	}

}
