package com.sino.product.dao;

import com.sino.product.entity.AttrGroupEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sino.product.vo.SpuItemAttrGroupVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:15:27
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

    List<SpuItemAttrGroupVo> getAttrGroupWithAttrBySpuId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}
