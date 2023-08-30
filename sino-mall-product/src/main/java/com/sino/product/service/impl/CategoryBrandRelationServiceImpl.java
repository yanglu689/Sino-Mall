package com.sino.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sino.product.dao.BrandDao;
import com.sino.product.dao.CategoryDao;
import com.sino.product.entity.BrandEntity;
import com.sino.product.entity.CategoryEntity;
import com.sino.product.service.BrandService;
import com.sino.product.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.CategoryBrandRelationDao;
import com.sino.product.entity.CategoryBrandRelationEntity;
import com.sino.product.service.CategoryBrandRelationService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    private BrandDao brandDao;

    @Autowired
    private CategoryDao categoryDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveDetail(CategoryBrandRelationEntity categoryBrandRelation) {

        // 取出前端传递过来的id
        Long brandId = categoryBrandRelation.getBrandId();
        Long catelogId = categoryBrandRelation.getCatelogId();

        QueryWrapper<CategoryBrandRelationEntity> queryWrapper = new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_Id", brandId).and(obj -> obj.eq("catelog_Id", catelogId));
        List<CategoryBrandRelationEntity> categoryBrandRelationEntitiesList = this.baseMapper.selectList(queryWrapper);

        if (!ObjectUtils.isEmpty(categoryBrandRelationEntitiesList)){
            throw new RuntimeException();
        }


        // 通过前端传过来的id查数据库对应的名称
        BrandEntity brandEntity = brandDao.selectById(brandId);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);

        // 设置属性
        categoryBrandRelation.setBrandName(brandEntity.getName());
        categoryBrandRelation.setCatelogName(categoryEntity.getName());

        // 保存数据
        this.save(categoryBrandRelation);
    }

    @Override
    @Transactional
    public void updateBrand(Long brandId, String name) {
//        new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_Id",brand.getBrandId()).set("name",brand.getName())
        CategoryBrandRelationEntity categoryBrandRelationEntity = new CategoryBrandRelationEntity();
        categoryBrandRelationEntity.setBrandId(brandId);
        categoryBrandRelationEntity.setBrandName(name);

        // 跟新关联关系表
        this.update(categoryBrandRelationEntity,new UpdateWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
    }

    @Override
    public List<BrandEntity> getbrandsByCatId(Long catId) {
        List<CategoryBrandRelationEntity> categoryBrandRelationEntityList = this.list(new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));
        if (ObjectUtils.isEmpty(categoryBrandRelationEntityList)){
            return null;
        }

        //获取品牌的id 和 品牌名
        List<BrandEntity> brandEntities = categoryBrandRelationEntityList.stream().map(catBrand -> {
            BrandEntity brandEntity = new BrandEntity();
            brandEntity.setBrandId(catBrand.getBrandId());
            brandEntity.setName(catBrand.getBrandName());
//            Long brandId = catBrand.getBrandId();
//            BrandEntity brandEntity = brandDao.selectById(brandId);
            return brandEntity;
        }).collect(Collectors.toList());

        return brandEntities;
    }

}