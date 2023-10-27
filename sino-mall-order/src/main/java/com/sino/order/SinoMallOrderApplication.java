package com.sino.order;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


/**
 * 信和商城订单申请
 *
 * 1. 引入amqp场景：rabbitAutoConfiguration就会自动生效
 * 2. 给容器中配置了 RabbitTemplate、 AmqpAdmin、CachingConnectFactory、RabbitMessageTemplate;
 * 3. 给配置文件中配置spring.rabbitmq信息
 * 4. @EnableRabbit:
 * 5. 监听消息，使用@RabbitListener; 必须有@EnableRabbit
 * @RabbitListener 类+方法上  监听那些队列
 * @RabbitHandler 方法上  ，根据类型重载
 *
 * 解决本地事务失效问题：
 *  1. 引用spring-boot-starter-aop -》aspectj
 *  2. 使用 @EnableAspectJAutoProxy(exposeProxy = true) 对外暴露代理对象 开启动态代理
 *  3. 本类互调使用动态代理对象
 *
 *  Seata控制分布式事务
 *  1. 每一个微服务必须先创建undo_log;
 *  2. 安装事务协调器：seata-server
 *  3. 整合
 *  	1. 导入依赖：spring-cloud-starter-alibaba-seata
 *  	2. 启动seata-server服务器	  1.52
 *  		registry.conf: 注册中心配置：
 *  	3. 所有想要用到分布式事务的微服务使用seata DataSourceProxy代理自己的数据源
 *		4. 每个微服务，都必须导入 registry.conf . file.conf
 *		5. 启动测试分布式事务
 *		6. 给分布式大事务的入口标注@GlobalTranscational
 *		7. 每一个远程的小事务用 @Transactional
 * @author yanglupc
 * @date 2023/10/04
 */
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableFeignClients
@EnableRedisHttpSession
@EnableRabbit  //作用是开启监听消息，如果只是发送消息，可以不适用这个注解
@EnableDiscoveryClient
@SpringBootApplication
public class SinoMallOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(SinoMallOrderApplication.class, args);
	}

}

