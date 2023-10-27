package com.sino.seckill.scheduled;

import com.sino.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品的定时上架
 * 每天晚上3点：上架最近三天需要秒杀的商品
 * 当天00：00：00 - 23：59：59
 * 明天00：00：00 - 23：59：59
 * 后天00：00：00 - 23：59：59
 * @author yanglupc
 * @date 2023/10/12
 */
@Slf4j
@Service
public class SeckillSkuScheduled {
    @Autowired
    private SeckillService secKillService;

    @Autowired
    private RedissonClient redissonClient;

    private final String upload_lock = "seckill:upload:lock";

    @Scheduled(cron = "*/3 * * * * ?")
    public void uploadSeckillSkuLatest3Days(){
        log.info("秒杀任务开启扫描 ..........");
        // TODO 幂等性处理
        // 获取分布式锁， 防止其他机器重复执行定时任务
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10L, TimeUnit.SECONDS);
        // 1. 重复上架无需处理
        try {
            secKillService.uploadSeckillSkuLatest3Days();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }

        log.info("秒杀任务扫描结束 ..........");

    }
}
