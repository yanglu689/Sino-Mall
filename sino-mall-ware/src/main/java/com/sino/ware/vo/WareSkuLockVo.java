package com.sino.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class WareSkuLockVo {
    private String orderSn;  //那个订单号
    private List<OrderItemVo> locks;  //需要锁那些库存信息
}
