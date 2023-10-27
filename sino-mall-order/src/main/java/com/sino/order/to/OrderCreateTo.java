package com.sino.order.to;

import com.sino.order.entity.OrderEntity;
import com.sino.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderCreateTo {
    private OrderEntity order; // 订单
    private List<OrderItemEntity> orderitems; // 订单中商品信息
    private BigDecimal payPrice; // 订单计算的应付价格
    private BigDecimal fare; // 运费

}
