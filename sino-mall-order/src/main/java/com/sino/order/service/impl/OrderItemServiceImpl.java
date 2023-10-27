package com.sino.order.service.impl;

import com.rabbitmq.client.Channel;
import com.sino.order.entity.OrderEntity;
import com.sino.order.entity.OrderReturnReasonEntity;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.order.dao.OrderItemDao;
import com.sino.order.entity.OrderItemEntity;
import com.sino.order.service.OrderItemService;

@RabbitListener(queues = {"hello.java.queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 接收消息
     * queues:声明需要监听的所有队列
     *
     * 参数可以写以下类型
     * @param message      消息
     * @param reasonEntity 原因实体 T 泛型
     * @param channel      当前传输数据的通道
     */
    // @RabbitListener(queues = {"hello.java.queue"})
    @RabbitHandler
    public  void receiveMessage(Message message, OrderReturnReasonEntity reasonEntity, Channel channel){

        System.out.println("收到消息。。。" + message+"====>"+reasonEntity);
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            channel.basicAck(deliveryTag, false); //手动签收
            // channel.basicNack(deliveryTag, false,true);  // b 是否批量拒收，b1 是否重新入队
            // channel.basicReject(deliveryTag,true);  // b是否入队
        } catch (IOException e) {

        }

    }

    @RabbitHandler
    public  void receiveMessage2(OrderEntity entity){

        System.out.println("====>"+entity);

    }

}