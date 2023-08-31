package com.sino.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sino.common.to.SkuReductionTo;
import com.sino.common.utils.PageUtils;
import com.sino.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 14:21:03
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo skuReductionTo);
}

