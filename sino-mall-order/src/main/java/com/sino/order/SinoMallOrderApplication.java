package com.sino.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class SinoMallOrderApplication {

	public static void main(String[] args) {
		SpringApplication.run(SinoMallOrderApplication.class, args);
	}

}
