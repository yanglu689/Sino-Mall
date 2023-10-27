package com.sino.seckill.to;

import com.sino.seckill.vo.SeckillSkuRelationVo;
import com.sino.seckill.vo.SkuInfoVo;
import lombok.Data;

@Data
public class SecKillSkuRedisTo {

    private SeckillSkuRelationVo relationVo;

    private SkuInfoVo skuInfoVo;

    // 开始时间
    private Long startTime;

    // 结束时间
    private Long endTime;

    private String randomCode;
}
