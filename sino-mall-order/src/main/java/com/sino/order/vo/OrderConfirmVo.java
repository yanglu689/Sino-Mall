package com.sino.order.vo;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {

    @Getter @Setter
    private List<MemberAddressVo> address;

    @Getter @Setter
    private List<OrderItemVo> items;

    // 发票信息

    // 优惠券信息 积分
    @Getter @Setter
    private Integer integration;

    // 防重令牌
    @Getter @Setter
    private String orderToken;

    @Getter @Setter
    private Map<Long, Boolean> stocks;

    // 订单总额
    // private BigDecimal total;

    // 支付金额
    // private BigDecimal payPrice;

    public Integer getCount(){
        Integer totalCount = 0;
        if (items != null) {
            for (OrderItemVo item : items) {
                Integer count = item.getCount();
                totalCount+=count;
            }
        }
        return totalCount;
    }


    public BigDecimal getTotal() {
        BigDecimal totalPrice = new BigDecimal("0");
        if (items != null) {
            for (OrderItemVo item : items) {
                BigDecimal price = item.getPrice().multiply(new BigDecimal(item.getCount().toString()));
                totalPrice = totalPrice.add(price);
            }
        }
        return totalPrice;
    }

    public BigDecimal getPayPrice() {
        return getTotal();
    }


}
