package com.sino.order.vo;

import com.sino.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {

    private OrderEntity order;
    private Integer code; //0成功，，其他为错误码
}
