package com.sino.seckill.service;

import com.sino.seckill.to.SecKillSkuRedisTo;

import java.util.List;

public interface SeckillService {
    void uploadSeckillSkuLatest3Days();

    List<SecKillSkuRedisTo> getCurrentSeckillSkus();

    SecKillSkuRedisTo getSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num);
}
