package com.sino.ware.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 消息回调配置
 *
 * @author yanglupc
 * @date 2023/10/04
 */
@Configuration
public class MessageCallbackConfig {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PostConstruct
    public void initRabbitTemplate(){

        //设置确认回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            /**
             * 确认
             *
             * @param correlationData 当前消息的唯一关联数据（这个是消息的唯一id）
             * @param ack             消息是否成功收到
             * @param cause           消息失败原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println("confirm==>CorrelationData["+correlationData+"]===>ack["+ack+"]===>cause["+cause+"]");
            }
        });

        //设置消息正确抵达队列回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 返回消息
             *
             * @param message    消息
             * @param replyCode  回复代码
             * @param replyText  回复文本
             * @param exchange   交换机
             * @param routingKey 路由key
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                System.out.println("returnCallback===>message["+message+"]==>replyCode["+replyCode+"]==>replyText["+replyText+"]==>["+exchange+"]==>["+routingKey+"]");
            }
        });

    }
}
