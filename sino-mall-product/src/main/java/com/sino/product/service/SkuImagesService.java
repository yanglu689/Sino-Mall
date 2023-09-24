package com.sino.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sino.common.utils.PageUtils;
import com.sino.product.entity.SkuImagesEntity;

import java.util.List;
import java.util.Map;

/**
 * sku图片
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:15:27
 */
public interface SkuImagesService extends IService<SkuImagesEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<SkuImagesEntity> getImagesBySkuId(Long skuId);
}

