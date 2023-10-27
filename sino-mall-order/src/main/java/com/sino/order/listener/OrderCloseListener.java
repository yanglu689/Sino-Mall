package com.sino.order.listener;

import com.rabbitmq.client.Channel;
import com.sino.order.entity.OrderEntity;
import com.sino.order.service.OrderService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RabbitListener(queues = {"order.release.order.queue"})
public class OrderCloseListener {

    @Autowired
    private OrderService orderService;

    @RabbitHandler
    public void  listener(Message message, OrderEntity orderEntity, Channel channel) throws IOException {
        System.out.println("收到过期订单信息：准备关闭订单");
        try {
            orderService.closeOrder(orderEntity);
            // 可以在此调用支付宝的收单功能
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }

    }
}


