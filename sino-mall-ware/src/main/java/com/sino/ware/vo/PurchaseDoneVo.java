package com.sino.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class PurchaseDoneVo {
    private Long id; //采购订单id
    private List<PurchaseItemDoneVo> items;
}
