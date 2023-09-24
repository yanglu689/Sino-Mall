package com.sino.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sino.common.utils.PageUtils;
import com.sino.product.entity.CategoryEntity;
import com.sino.product.vo.Catelog2Vo;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:15:27
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByCatIds(List<Long> asList);

    Long[] getCateLogIds(Long catelogId);

    void updateDetail(CategoryEntity category);

    List<CategoryEntity> getLevel1Categorys();

    Map<String, List<Catelog2Vo>> getCatalogJson();
}

