package com.sino.seckill.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 开启定时任务
 * 开启异步
 */
@Configuration
@EnableAsync
@EnableScheduling
public class ScheduleConfig {
}
