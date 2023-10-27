package com.sino.product.feign;

import com.sino.common.utils.R;
import com.sino.product.fallback.SeckillFeignServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *  消费端处调用秒杀服务熔断/降级后的处理
 *  fallback = SeckillFeignServiceFallback.class
 */
@FeignClient(value = "sino-mall-seckill", fallback = SeckillFeignServiceFallback.class)
public interface SeckillFeignService {

    @GetMapping("/sku/seckill")
    R getSeckillInfo(@RequestParam Long skuId);
}
