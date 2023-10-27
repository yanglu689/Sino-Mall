package com.sino.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {

    private Long addrId; // 收货地址id

    private Integer payType; //支付方式

    private String orderToken; // 防重令牌

    private BigDecimal payPrice; // 应付价格 验价

    // 用户相关信息都在session中，可以直接取出

    private String note; //订单备注
}
