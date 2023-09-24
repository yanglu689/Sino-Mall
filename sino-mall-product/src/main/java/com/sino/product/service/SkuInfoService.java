package com.sino.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sino.common.utils.PageUtils;
import com.sino.product.entity.SkuInfoEntity;
import com.sino.product.vo.SkuItemVo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * sku信息
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:15:27
 */
public interface SkuInfoService extends IService<SkuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageByCondition(Map<String, Object> params);

    List<SkuInfoEntity> getSkuIdBySpuId(Long spuId);

    SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException;
}

