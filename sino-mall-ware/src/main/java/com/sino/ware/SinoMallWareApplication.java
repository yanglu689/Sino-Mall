package com.sino.ware;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableRabbit
@EnableFeignClients(basePackages = "com.sino.ware.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class SinoMallWareApplication {

    public static void main(String[] args) {
        SpringApplication.run(SinoMallWareApplication.class, args);
    }

}
