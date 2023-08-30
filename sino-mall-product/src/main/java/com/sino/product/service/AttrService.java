package com.sino.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sino.common.utils.PageUtils;
import com.sino.product.entity.AttrEntity;
import com.sino.product.vo.AttrRespVo;
import com.sino.product.vo.AttrVo;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:15:27
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils getBaseAttrPage(Map params, Long catId, String type);

    AttrRespVo getAttrInfo(Long attrId);

    void updateDetail(AttrVo attrVo);

    List<AttrEntity> getRelationAttr(Long attrgroupId);
}

