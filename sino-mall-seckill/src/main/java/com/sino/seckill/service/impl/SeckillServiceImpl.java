package com.sino.seckill.service.impl;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.google.common.collect.ImmutableSet;
import com.sino.common.to.mq.SeckillOrderTo;
import com.sino.common.utils.R;
import com.sino.common.vo.MemberRespVo;
import com.sino.seckill.Interceptor.LoginUserInterceptor;
import com.sino.seckill.feign.CouponFeignService;
import com.sino.seckill.feign.ProductFeignService;
import com.sino.seckill.service.SeckillService;
import com.sino.seckill.to.SecKillSkuRedisTo;
import com.sino.seckill.vo.SeckillSessionsWithSkus;
import com.sino.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final String SESSION_CACHE_PREFIX = "seckill:sessions:";
    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";
    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 1.扫描最近需要参与秒杀的商品
        R r = couponFeignService.getLatest3DaySession();
        if (r.getCode() == 0) {
            // 上架商品
            List<SeckillSessionsWithSkus> sessionsWithSkus = r.getData(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });

            // 缓存到redis
            saveSessionInfo(sessionsWithSkus);

            // 保存活动商品的基本信息
            saveSessionSkuInfos(sessionsWithSkus);

        }

    }

    /**
     * 获取当前秒杀SKU
     *
     * @return {@link List}<{@link SecKillSkuRedisTo}>
     */
    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        // 获取当前时间
        long currTime = new Date().getTime();
        Set<String> keys = redisTemplate.keys(SESSION_CACHE_PREFIX + "*");
        List<String> range = new ArrayList<>();
        for (String key : keys) {
            String timeStr = key.replace(SESSION_CACHE_PREFIX, "");
            String[] timeArr = timeStr.split("_");
            Long start = Long.parseLong(timeArr[0]);
            Long end = Long.parseLong(timeArr[1]);
            if (currTime > start && currTime < end) {
                // 取出活动场次对应的keys
                range = redisTemplate.opsForList().range(key, -100, 100);
                break;
            }
        }


        if (!ObjectUtils.isEmpty(range)) {
            BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            List<String> skuToJsons = hashOps.multiGet(range);
            List<SecKillSkuRedisTo> collect = skuToJsons.stream().map(skuToJson -> JSON.parseObject(skuToJson, SecKillSkuRedisTo.class)).collect(Collectors.toList());
            return collect;
        }

        return null;
    }


    /**
     * 降级处理方法
     *
     * @param skuId 货号
     * @param e     e
     * @return {@link SecKillSkuRedisTo}
     */
    public SecKillSkuRedisTo blockHandler(Long skuId, BlockException e){
        log.info("skuid:{},e:{}", skuId,e);
        return null;
    }

    @SentinelResource(value = "getSeckillInfo", blockHandler = "blockHandler")
    @Override
    public SecKillSkuRedisTo getSeckillInfo(Long skuId) {
        // 使用try-catch方式降级
        try (Entry entry = SphU.entry("seckillSkus")) {
            long now = new Date().getTime();
            // 拿到所有的keys
            BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
            Set<String> keys = hashOps.keys();
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String seckillJson = hashOps.get(key);
                    SecKillSkuRedisTo skuRedisTo = JSON.parseObject(seckillJson, SecKillSkuRedisTo.class);
                    if (now < skuRedisTo.getStartTime() || now > skuRedisTo.getEndTime()) {
                        skuRedisTo.setRandomCode("");
                    }
                    return skuRedisTo;
                }
            }
        } catch (BlockException e) {
            log.info("try-catch服务getSeckillInfo降级：{}", e.getMessage());
        }
        return null;
    }

    /**
     * 商品秒杀处理
     *
     * @param killId 杀死 ID
     * @param key    钥匙
     * @param num    数字
     * @return {@link String}
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        long l = System.currentTimeMillis();
        MemberRespVo loginUser = LoginUserInterceptor.loginUser.get();
        BoundHashOperations<String, String, String> hashOps = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        String skuJson = hashOps.get(killId);
        // 1. 校验场次商品是否存在
        if (StringUtils.hasText(skuJson)) {
            SecKillSkuRedisTo skuRedisTo = JSON.parseObject(skuJson, SecKillSkuRedisTo.class);
            // 2. 校验是否在秒杀时间段内
            long now = new Date().getTime();
            Long startTime = skuRedisTo.getStartTime();
            Long endTime = skuRedisTo.getEndTime();
            if (now >= startTime && now <= endTime) {
                // 3. 校验随机token 和skuKey是否一致
                String prid_skuId = skuRedisTo.getRelationVo().getPromotionSessionId() + "_" + skuRedisTo.getRelationVo().getSkuId();
                if (key.equals(skuRedisTo.getRandomCode()) && killId.equals(prid_skuId)) {
                    // 4. 校验当前是否超过限制购买的数量
                    if (num <= skuRedisTo.getRelationVo().getSeckillLimit()) {
                        // 5. 校验当前用户是否重复购买, 幂等校验, 并设置过期时间
                        String existKey = loginUser.getId() + "_" + prid_skuId;
                        Long ttl = endTime - startTime;
                        Boolean isExit = redisTemplate.opsForValue().setIfAbsent(existKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (isExit) {
                            // 获取信号量，准备扣减库存
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + key);
                            try {
                                // 扣减库存，并且判断是否扣减成功，， 扣减库存是一个原子性的操作， 每次只允许一个线程/用户扣减
                                boolean isReduce = semaphore.tryAcquire(num, 300, TimeUnit.MILLISECONDS);
                                if (isReduce) {
                                    String idStr = IdWorker.getTimeId();
                                    // 扣减库存成功后，开发消息到mq，让订单系统异步处理创建订单。达到削峰的目的
                                    SeckillOrderTo seckillOrderTo = new SeckillOrderTo();
                                    seckillOrderTo.setOrderSn(idStr);
                                    seckillOrderTo.setNum(num);
                                    seckillOrderTo.setPromotionSessionId(skuRedisTo.getRelationVo().getPromotionSessionId());
                                    seckillOrderTo.setSeckillPrice(skuRedisTo.getRelationVo().getSeckillPrice());
                                    seckillOrderTo.setMemberId(loginUser.getId());
                                    // 发送消息，准备创建订单
                                    rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.order", seckillOrderTo);
                                    long l2 = System.currentTimeMillis();
                                    log.info("执行秒杀程序耗时：{}", l2 - l);
                                    return idStr;
                                }
                            } catch (InterruptedException e) {
                                return null;
                            }
                        }

                    }

                }
            }

        }
        return null;
    }

    /**
     * 保存活动信息
     *
     * @param sessionsWithSkus 与 SKU 会话
     */
    private void saveSessionInfo(List<SeckillSessionsWithSkus> sessionsWithSkus) {
        if (!ObjectUtils.isEmpty(sessionsWithSkus)) {
            sessionsWithSkus.forEach(session -> {
                //获取活动的开始时间和结束时间
                long start = session.getStartTime().getTime();
                long end = session.getEndTime().getTime();
                //拼接key
                String key = SESSION_CACHE_PREFIX + start + "_" + end;
                Boolean aBoolean = redisTemplate.hasKey(key);
                if (!aBoolean) {
                    List<String> skuIds = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId() + "_" + item.getSkuId()).collect(Collectors.toList());
                    //将skuIds放入key对应的list中
                    if (!ObjectUtils.isEmpty(skuIds)) {
                        redisTemplate.opsForList().leftPushAll(key, skuIds);
                    }
                }
            });
        }
    }

    /**
     * 保存活动 SKU 信息
     *
     * @param sessionsWithSkus 与 SKU 会话
     */
    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessionsWithSkus) {
        if (!ObjectUtils.isEmpty(sessionsWithSkus)) {
            sessionsWithSkus.forEach(session -> {
                // 绑定hash键
                BoundHashOperations<String, Object, Object> opsForHash = redisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                session.getRelationSkus().forEach(item -> {
                    Long skuId = item.getSkuId();
                    Boolean aBoolean = opsForHash.hasKey(item.getPromotionSessionId() + "_" + skuId);
                    if (!aBoolean) {
                        // 缓存商品详细信息
                        SecKillSkuRedisTo skuRedisTo = new SecKillSkuRedisTo();
                        skuRedisTo.setRelationVo(item);
                        // 远程查询商品服务的商品信息
                        R r = productFeignService.getSkuInfoBySkuId(skuId);
                        if (r.getCode() == 0) {
                            SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                            });
                            skuRedisTo.setSkuInfoVo(skuInfo);
                        }

                        // 设置当前商品秒杀时间信息
                        skuRedisTo.setStartTime(session.getStartTime().getTime());
                        skuRedisTo.setEndTime(session.getEndTime().getTime());

                        // 设置随机码， 防止恶意秒杀行为
                        String token = UUID.randomUUID().toString().replace("-", "");
                        skuRedisTo.setRandomCode(token);

                        // 使用库存作为分布式信号量   限流
                        RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                        // 商品秒杀的数量作为信号量
                        semaphore.trySetPermits(item.getSeckillCount());

                        String jsonString = JSON.toJSONString(skuRedisTo);
                        opsForHash.put(item.getPromotionSessionId() + "_" + skuId, jsonString);
                    }
                });
            });
        }
    }
}
