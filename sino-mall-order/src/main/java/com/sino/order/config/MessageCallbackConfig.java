package com.sino.order.config;

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
             * 只要消息抵达broker就ack=true
             * @param correlationData 当前消息的唯一关联数据（这个是消息的唯一id）
             * @param ack             消息是否成功收到
             * @param cause           消息失败原因
             */
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                // 1. 消息做好消息确认机制（pulisher, consumer [手动确认ack]）
                // 2. 每一个发送的消息都在数据库做好记录，定期将失败的消息重新发送
                // 服务器收到了
                // 修改消息的状态
                System.out.println("confirm==>CorrelationData["+correlationData+"]===>ack["+ack+"]===>cause["+cause+"]");
            }
        });

        //设置消息正确抵达队列回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            /**
             * 返回消息
             *
             * @param message    投递失败的消息相信信息
             * @param replyCode  回复状态码
             * @param replyText  回复文本内容
             * @param exchange   交当时这个消息发送给哪个交换机
             * @param routingKey 当时这个消息用的哪个路由key
             */
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                // 将rabbitmq失败消息保存到数据库
                System.out.println("returnCallback===>message["+message+"]==>replyCode["+replyCode+"]==>replyText["+replyText+"]==>["+exchange+"]==>["+routingKey+"]");
            }
        });

    }
}
