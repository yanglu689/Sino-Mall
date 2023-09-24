package com.sino.product.dao;

import com.sino.common.constant.ProductConstant;
import com.sino.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 * 
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:15:27
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    void updateSpuStatus(@Param("spuId") Long spuId, @Param("spuStatus") int spuStatus);
}
