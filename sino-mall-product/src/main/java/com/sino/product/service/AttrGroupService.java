package com.sino.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sino.common.utils.PageUtils;
import com.sino.product.entity.AttrEntity;
import com.sino.product.entity.AttrGroupEntity;
import com.sino.product.vo.AttrAttrGroupRelationVo;
import com.sino.product.vo.AttrGroupVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:15:27
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);

    List<AttrEntity> getAttrListByAttrGroupId(Long attrGroupId);

    void deleteRelation(AttrAttrGroupRelationVo[] relationVo);

    PageUtils findCanBindAttrList(Map params, Long attrGroupId);

    void addAttrAttrGroup(List<AttrAttrGroupRelationVo> vo);

    List<AttrGroupVo> getAttrGroupWithAttr(Long catelogId);
}

