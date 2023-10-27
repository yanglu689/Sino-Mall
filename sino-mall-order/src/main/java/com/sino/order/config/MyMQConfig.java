package com.sino.order.config;

import com.rabbitmq.client.Channel;
import com.sino.order.entity.OrderEntity;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyMQConfig {

    // @RabbitListener(queues = {"order.release.order.queue"})
    // public void  listener(Message message, OrderEntity orderEntity, Channel channel) throws IOException {
    //     System.out.println("收到过期订单信息：准备关闭订单");
    //     channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    // }

    // Bean Binding Queue Exchange

    /**
     * 容器中的 Binding Queue Exchange 都会自动创建
     * rabbitMQ 只要有， @Bean 声明属性发生变化也不会覆盖
     * @return
     */
    @Bean
    public Queue orderDelayQueue(){
        /**
         * x-dead-letter-exchange: order-event-exchange
         * x-dead-letter-routing-key: order.release.order
         * x-message-ttl: 60000
         */
        Map<String, Object> param = new HashMap<>();
        param.put("x-dead-letter-exchange","order-event-exchange");
        param.put("x-dead-letter-routing-key","order.release.order");
        param.put("x-message-ttl",60000);

        // String name, boolean durable, boolean exclusive, boolean autoDelete, @Nullable Map<String, Object> arguments
        Queue queue = new Queue("order.delay.queue", true, false, false, param);
        return queue;
    }

    @Bean
    public Queue orderReleaseOrderQueue(){
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Exchange orderEventExchange(){
        // String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        return new TopicExchange("order-event-exchange", true, false);
    }

    @Bean
    public Binding orderCreateOrderBinding(){
        //String destination, Binding.DestinationType destinationType, String exchange, String routingKey, @Nullable Map<String, Object> arguments
        return new Binding("order.delay.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.create.order", null);
    }

    @Bean
    public Binding orderReleaseOrderBinding(){
        return new Binding("order.release.order.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.release.order", null);
    }

    @Bean
    public Binding orderReleaseOtherBinding(){
        return new Binding("stock.release.stock.queue", Binding.DestinationType.QUEUE, "order-event-exchange", "order.release.other.#", null);
    }


    @Bean
    public Queue orderSeckillOrderQueue(){
        //String name, boolean durable, boolean exclusive （排他）, boolean autoDelete
        return new Queue("order.seckill.order.queue",true, false,false);
    }

    @Bean
    public Binding orderSeckillOrderQueueBinding(){
        //String destination, Binding.DestinationType destinationType, String exchange, String routingKey, @Nullable(不可没有) Map<String, Object> arguments
        return new Binding("order.seckill.order.queue", Binding.DestinationType.QUEUE, "order-event-exchange","order.seckill.order",null);
    }

}
