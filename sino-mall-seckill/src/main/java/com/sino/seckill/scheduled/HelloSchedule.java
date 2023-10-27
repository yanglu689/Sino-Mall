package com.sino.seckill.scheduled;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
// @EnableAsync
// @EnableScheduling
public class HelloSchedule {

    /**
     *  1. spring中6位组成，不允许7位的年
     *  2. 在周几的位置，1-7代表周一到周日；MON-SUN  quartz中1-7代表周日到周六
     *  3. 定时任务不应该阻塞，默认是阻塞的
     *       1. 可以让业务运行以异步的方式，自己提交到线程池
     *        CompletableFuture.runAsync(()->{
     *                  执行异步代码
     *            }, executor
     *        });
     *        2. 支持定时任务线程池，设置 TaskSchedulingProperties;
     *              spring.task.scheduling.pool.size=5
     *        3. 让定时任务异步执行
     *          异步任务；
     *        解决 ： 使用异步+定时任务来完成定时任务不阻塞功能
     *
     * @throws InterruptedException
     */
    // @Async // 开启异步任务
    // @Scheduled(cron = "* * * * * 4")
    public void sayHello() throws InterruptedException {
        System.out.println("hello,world");
        Thread.sleep(3000);
    }
}
