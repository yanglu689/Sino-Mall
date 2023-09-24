package com.sino.product.service.impl;

import com.sino.product.service.CategoryBrandRelationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.BrandDao;
import com.sino.product.entity.BrandEntity;
import com.sino.product.service.BrandService;
import org.springframework.transaction.annotation.Transactional;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<BrandEntity> brandEntityQueryWrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            brandEntityQueryWrapper.and(obj -> obj.eq("brand_Id", key).or().like("name", key).or().like("descript", key));
        }
        IPage<BrandEntity> page = this.page(
                new Query<BrandEntity>().getPage(params),
                brandEntityQueryWrapper
        );

        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void updateDetailById(BrandEntity brand) {
        //跟新主表数据
        this.updateById(brand);
        if (StringUtils.isNotEmpty(brand.getName())){
            //根据关联表的冗余字段品牌名称
            categoryBrandRelationService.updateBrand(brand.getBrandId(),brand.getName());

            //TODO 跟新其他关联信息
        }

    }

    @Override
    public List<BrandEntity> getBrandInfos(List<Long> brandIds) {
        List<BrandEntity> brandEntities = baseMapper.selectList(new QueryWrapper<BrandEntity>().in("brand_Id", brandIds));
        return brandEntities;
    }

}