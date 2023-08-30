package com.sino.product;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@MapperScan("com.sino.product.dao")
@SpringBootApplication
public class SinoMallProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(SinoMallProductApplication.class, args);
    }

}
