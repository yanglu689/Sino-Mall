package com.sino.ware.vo;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;

@Data
public class SkuHasStockVo {
    private Long skuId;
    private Boolean hasStock;
}
