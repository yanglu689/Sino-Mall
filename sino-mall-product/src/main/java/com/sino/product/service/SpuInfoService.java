package com.sino.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sino.common.utils.PageUtils;
import com.sino.product.entity.SpuInfoEntity;
import com.sino.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:15:27
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void savaSpuInfo(SpuSaveVo spuSaveVo);

    void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

    void up(Long spuId);
}

