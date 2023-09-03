package com.sino.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sino.common.constant.ProductConstant;
import com.sino.product.dao.AttrAttrgroupRelationDao;
import com.sino.product.entity.*;
import com.sino.product.service.AttrGroupService;
import com.sino.product.service.CategoryService;
import com.sino.product.service.ProductAttrValueService;
import com.sino.product.vo.AttrRespVo;
import com.sino.product.vo.AttrVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.AttrDao;
import com.sino.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void saveAttr(AttrVo attr) {
        AttrEntity entity = new AttrEntity();
        BeanUtils.copyProperties(attr, entity);

        // 保存属性信息
        this.save(entity);

        if (attr.getAttrGroupId() != null){
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrAttrgroupRelationEntity.setAttrId(entity.getAttrId());
            // 保存属性分组关联关系
            attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
        }
    }

    /**
     * 得到基本属性列表页
     *
     * @param params 参数
     * @param catId  分类id
     * @param type
     * @return {@link PageUtils}
     */

    @Override
    public PageUtils getBaseAttrPage(Map params, Long catId, String type) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq("attr_type", "base".equals(type) ? ProductConstant.AttrTypeEnum.ATTR_BASE_TYPE.getCode() : ProductConstant.AttrTypeEnum.ATTR_SALE_TYPE.getCode());
        String key = (String) params.get("key");
        // 根据catid判断查询情况 catId != 0
        if (catId != 0) {
            queryWrapper.eq("catelog_id", catId);
        }

        // 如果key 不为空
        if (StringUtils.isNotEmpty(key)) {
            queryWrapper.and(obj -> obj.eq("attr_id", key).or().like("attr_name", key));
        }

        // 获取到分页数据
        IPage page = this.page(new Query<AttrEntity>().getPage(params), queryWrapper);
        // 使用分页工具类整理分页数据
        PageUtils pageUtils = new PageUtils(page);
        // 获取每一条数据
        List<AttrEntity> records = (List<AttrEntity>) pageUtils.getList();
        // 为每一条数据设置分类和属性组等其他信息
        pageUtils.setList(setDetailInfo(records));
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo attrRespVo = new AttrRespVo();
        // 基本属性查询
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, attrRespVo);

        // 获取属性分组id
        if (attrEntity.getAttrType() == ProductConstant.AttrTypeEnum.ATTR_BASE_TYPE.getCode()){
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrRespVo.getAttrId()));
            if (!ObjectUtils.isEmpty(attrAttrgroupRelationEntity) && attrAttrgroupRelationEntity.getAttrGroupId() != null) {
                //设置属性组id
                attrRespVo.setAttrGroupId(attrAttrgroupRelationEntity.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupService.getById(attrAttrgroupRelationEntity.getAttrGroupId());
                if (!ObjectUtils.isEmpty(attrGroupEntity)) {
                    // 设置属性组名称
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }

        // 获取分类路径id
        Long[] cateLogIds = categoryService.getCateLogIds(attrRespVo.getCatelogId());
        attrRespVo.setCatelogPath(cateLogIds);

        return attrRespVo;
    }

    @Override
    public void updateDetail(AttrVo attrVo) {
        AttrEntity attr = new AttrEntity();
        BeanUtils.copyProperties(attrVo, attr);
        this.updateById(attr);
        // 修改属性和属性分组关联表中的分组id
        if (attr.getAttrType() == ProductConstant.AttrTypeEnum.ATTR_BASE_TYPE.getCode() && attrVo.getAttrGroupId() != null){
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrId(attr.getAttrId());
            attrAttrgroupRelationEntity.setAttrGroupId(attrVo.getAttrGroupId());

            Integer count = attrAttrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            if (count <= 0) {
                attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
            } else {
                attrAttrgroupRelationDao.update(attrAttrgroupRelationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_Id", attrVo.getAttrId()));
            }
        }

    }

    /**
     * 设置属性列表详细信息
     *
     * @param records 记录
     * @return
     */

    private List<?> setDetailInfo(List<AttrEntity> records) {
        // 使用流式编程遍历列表
        List<AttrRespVo> respVoList = records.stream().map(attr -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attr, attrRespVo);
            // 设置分类名称
            CategoryEntity categoryEntity = categoryService.getById(attr.getCatelogId());
            if (!ObjectUtils.isEmpty(categoryEntity)) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }

            // 查找属性-属性组关联表的属性组id
            if (attr.getAttrType() == ProductConstant.AttrTypeEnum.ATTR_BASE_TYPE.getCode()){
                AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
                if (!ObjectUtils.isEmpty(attrAttrgroupRelationEntity) && attrAttrgroupRelationEntity.getAttrGroupId() != null) {
                    // 通过属性组id 到属性表查找指定的行记录
                    AttrGroupEntity attrGroupEntity = attrGroupService.getById(attrAttrgroupRelationEntity.getAttrGroupId());
                    if (!ObjectUtils.isEmpty(attrGroupEntity)) {
                        // 设置属性组名称
                        attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                    }
                }
            }
            return attrRespVo;
        }).collect(Collectors.toList());

        return respVoList;
    }

    /**
     * 根据分组id查找关联的所有基本属性
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> entities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));

        List<Long> attrIds = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds == null || attrIds.size() == 0){
            return null;
        }
        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);
        return (List<AttrEntity>) attrEntities;
    }

    @Override
    public List<ProductAttrValueEntity> listForSpu(Long spuId) {
        if (spuId == null){
            return  null;
        }
        List<ProductAttrValueEntity> productAttrValueEntities = productAttrValueService.list(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId));
        return productAttrValueEntities;
    }

    @Override
    public void updateBySpuId(List<ProductAttrValueEntity> attrList, Long spuId) {
        productAttrValueService.updateSpuAttr(attrList, spuId);
    }


}