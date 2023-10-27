package com.sino.common.to.mq;

import lombok.Data;

@Data
public class StockLockedTo {
    private Long id; //工作单的id
    private StockDetailTo detail; //工作单详情id
}
