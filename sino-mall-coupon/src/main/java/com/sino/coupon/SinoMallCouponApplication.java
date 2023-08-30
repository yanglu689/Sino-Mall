package com.sino.coupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class SinoMallCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(SinoMallCouponApplication.class, args);
    }

}
