package com.sino.product.vo;

import com.sino.product.entity.SkuImagesEntity;
import com.sino.product.entity.SkuInfoEntity;
import com.sino.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    // 1. sku基本信息获取 pms_sku_info
    private SkuInfoEntity info;

    // 是否有库存
    private  boolean hasStock = true;

    // 2. sku的图片信息 pms_sku_images
    private List<SkuImagesEntity> images;

    // 3. 获取spu的销售属性组合   pms_sku_sale_attr_value
    private List<SkuItemSaleAttrVo> saleAttrs;

    // 4. 获取spu的介绍
    private SpuInfoDescEntity spuDesc;

    // 5. 获取spu的规格参数信息
    private List<SpuItemAttrGroupVo> groupAttrs;

    // 6. 设置秒杀信息
    private SecKillSkuRedisVo redisVo;


}
