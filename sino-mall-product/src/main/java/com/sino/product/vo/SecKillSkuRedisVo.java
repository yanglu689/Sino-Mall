package com.sino.product.vo;

import lombok.Data;

@Data
public class SecKillSkuRedisVo {

    private SeckillSkuRelationVo relationVo;

    // 开始时间
    private Long startTime;

    // 结束时间
    private Long endTime;

    private String randomCode;
}
