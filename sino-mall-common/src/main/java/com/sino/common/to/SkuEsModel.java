package com.sino.common.to;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * "attrValue": { "type": "keyword"
 */
@Data
public class SkuEsModel {

    /**
     * skuId
     */
    private Long skuId;

    /**
     * spuId
     */
    private Long spuId;

    /**
     * sku标题
     */
    private String skuTitle;

    /**
     * sku商品价格
     */
    private BigDecimal skuPrice;

    /**
     * sku商品图片
     */
    private String skuImg;

    /**
     * 销售数量
     */
    private Long saleCount;

    /**
     * 是否还有库存
     */
    private Boolean hasStock;

    /**
     * 得分
     */
    private Long hotScore;

    /**
     * 品牌id
     */
    private Long brandId;

    /**
     * 类别id
     */
    private Long catalogId;

    /**
     * 品牌名称
     */
    private String brandName;

    /**
     * 品牌loge
     */
    private String brandImg;

    /**
     * 类别名称
     */
    private String catalogName;

    /**
     * skp规格属性列表
     */
    private List<Attr> attrs;

    @Data
    public static class Attr {
        private Long attrId;

        private String attrName;

        private String attrValue;

    }
}
