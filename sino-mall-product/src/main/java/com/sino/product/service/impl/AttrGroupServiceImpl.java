package com.sino.product.service.impl;

import com.sino.common.constant.ProductConstant;
import com.sino.product.dao.AttrDao;
import com.sino.product.entity.AttrAttrgroupRelationEntity;
import com.sino.product.entity.AttrEntity;
import com.sino.product.service.AttrAttrgroupRelationService;
import com.sino.product.service.AttrGroupService;
import com.sino.product.service.AttrService;
import com.sino.product.vo.AttrAttrGroupRelationVo;
import com.sino.product.vo.AttrGroupVo;
import com.sino.product.vo.AttrVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.AttrGroupDao;
import com.sino.product.entity.AttrGroupEntity;
import org.springframework.util.ObjectUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrAttrgroupRelationService attrAttrgroupRelationService;

    @Autowired
    private AttrDao attrDao;

    @Autowired
    @Lazy
    private AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        IPage<AttrGroupEntity> page;
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if (catelogId != 0) {
            // select * from pms_attr_group where catelog_id = ? and (attr_group_id=key or attr_group_name like '%key%')
            wrapper = new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId);
        }

        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            wrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }

        page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
        return new PageUtils(page);
    }

    @Override
    public List<AttrEntity> getAttrListByAttrGroupId(Long attrGroupId) {
        List<AttrAttrgroupRelationEntity> entityList = attrAttrgroupRelationService.list(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupId));

        //查出属性组关联的所有属性id
        List<Long> attrIds = entityList.stream().map(item -> item.getAttrId()).distinct().collect(Collectors.toList());

        List<AttrEntity> attrEntities = new ArrayList<>();
        if (!ObjectUtils.isEmpty(attrIds)) {
            attrEntities = attrDao.selectBatchIds(attrIds);
        }
        return attrEntities;
    }

    @Override
    public void deleteRelation(AttrAttrGroupRelationVo[] relationVo) {
        List<AttrAttrgroupRelationEntity> relationEntityList = Arrays.asList(relationVo).stream().map(item -> {
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, attrAttrgroupRelationEntity);
            return attrAttrgroupRelationEntity;
        }).collect(Collectors.toList());

        if (!ObjectUtils.isEmpty(relationEntityList)) {
            // 根据attrid attgroupid 批量删除
            attrAttrgroupRelationService.deleteBatchRelation(relationEntityList);
        }
    }

    @Override
    public PageUtils findCanBindAttrList(Map params, Long attrGroupId) {

        // 通过attrGroupId找到三级分类的id
        AttrGroupEntity attrGroupEntity = this.getById(attrGroupId);
        if (ObjectUtils.isEmpty(attrGroupEntity)) {
            return null;
        }

        //三级分类id
        Long catelogId = attrGroupEntity.getCatelogId();

        // 通过三级分类id 找到这些分类下所有关联的属性分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        // 收集属性组id
        List<Long> attrGroupIds = new ArrayList<>();
        if (!ObjectUtils.isEmpty(attrGroupEntities)) {
            attrGroupIds = attrGroupEntities.stream().map(item -> item.getAttrGroupId()).collect(Collectors.toList());
        }
        // 通过属性分组id找到关联表下说有被关联的属性
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationService.list(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", attrGroupIds));

        // 收集属性id
        List<Long> attrIds = new ArrayList<>();
        if (!ObjectUtils.isEmpty(relationEntities)) {
            attrIds = relationEntities.stream().map(item -> item.getAttrId()).collect(Collectors.toList());
        }

        QueryWrapper<AttrEntity> attrEntityQueryWrapper = new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId).eq("attr_type", ProductConstant.AttrTypeEnum.ATTR_BASE_TYPE.getCode());

        if (!ObjectUtils.isEmpty(attrIds)) {
            attrEntityQueryWrapper.notIn("attr_id", attrIds);
        }

        // 处理查询条件
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            attrEntityQueryWrapper.and(w -> w.eq("attr_id", key).or().like("attr_name", key));
        }

        // 分页查询
        IPage iPage = attrDao.selectPage(new Query<AttrEntity>().getPage(params), attrEntityQueryWrapper);
        PageUtils pageUtils = new PageUtils(iPage);
        return pageUtils;
    }

    @Override
    public void addAttrAttrGroup(List<AttrAttrGroupRelationVo> vo) {
        List<AttrAttrgroupRelationEntity> entityList = vo.stream().map(relationVo -> {
            AttrAttrgroupRelationEntity entity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(relationVo, entity);
            return entity;
        }).collect(Collectors.toList());

        attrAttrgroupRelationService.saveBatch(entityList);

    }

    @Override
    public List<AttrGroupVo> getAttrGroupWithAttr(Long catelogId) {
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        if (ObjectUtils.isEmpty(attrGroupEntities)) {
            return null;
        }

        List<AttrGroupVo> attrGroupVos = attrGroupEntities.stream().map(item -> {
            List<AttrEntity> attrEntities = new ArrayList<>();
            AttrGroupVo vo = new AttrGroupVo();
            BeanUtils.copyProperties(item, vo);
            // 设置属性组绑定的所有基本属性
            if (vo.getAttrGroupId() != null) {
//                List<AttrAttrgroupRelationEntity> list = attrAttrgroupRelationService.list(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", vo.getAttrGroupId()));
//                // 查出绑定的所有的属性id
//                List<Long> attrIds = list.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
//                if (!ObjectUtils.isEmpty(attrIds)) {
//                    //attrDao.selectBatchIds(attrIds);
//                    attrEntities = attrDao.selectList(new QueryWrapper<AttrEntity>().eq("attr_type", ProductConstant.AttrTypeEnum.ATTR_BASE_TYPE.getCode()).in("attr_id", attrIds));
//                }

                attrEntities = attrService.getRelationAttr(vo.getAttrGroupId());

                List<AttrVo> attrVos = new ArrayList<>();
                // 转换成attrvo
                if (!ObjectUtils.isEmpty(attrEntities)) {
                    attrVos = attrEntities.stream().map(attr -> {
                        AttrVo attrVo = new AttrVo();
                        BeanUtils.copyProperties(attr, attrVo);
                        return attrVo;
                    }).collect(Collectors.toList());
                }
                vo.setAttrs(attrVos);
            }
            return vo;
        }).collect(Collectors.toList());

        return attrGroupVos;
    }
}