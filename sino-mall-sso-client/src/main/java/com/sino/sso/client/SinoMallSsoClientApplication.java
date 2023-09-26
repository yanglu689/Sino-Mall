package com.sino.sso.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 1. 给登录服务器留下登录痕迹
 * 2. 登录服务器要将token信息重定向的时候，带到url地址上
 * 3. 其他系统要处理url地址上的关键token, 只要有，将token对应的用户保存到自己的session中
 * 4. 自己系统将用户保存在自己的会话中。
 */
@SpringBootApplication
public class SinoMallSsoClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SinoMallSsoClientApplication.class, args);
    }

}
