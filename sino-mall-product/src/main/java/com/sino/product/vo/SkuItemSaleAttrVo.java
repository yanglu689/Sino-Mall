package com.sino.product.vo;

import lombok.Data;

import java.util.List;

@Data
public class SkuItemSaleAttrVo {
    /**
     * 属性id
     */
    private Long attrId;

    private String attrName;

    /**
     * 属性值
     */
    private List<AttrValuesWithSkuId> attrValues;

}
