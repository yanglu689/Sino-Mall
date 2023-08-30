package com.sino.product.service.impl;

import com.sino.product.dao.CategoryBrandRelationDao;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.CategoryDao;
import com.sino.product.entity.CategoryEntity;
import com.sino.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationDao categoryBrandRelationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 树型列表
     *
     * @return {@link List}<{@link CategoryEntity}>
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        // 查询所有的菜单
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        // 将菜单按照树形结构存放
        List<CategoryEntity> categoryEntityTree = categoryEntities.stream()
                .filter(item -> item.getParentCid() == 0)
                .map(menu -> {
                    //设置子菜单
                    menu.setChildren(setChildern(menu, categoryEntities));
                    return menu;
                })
                // 按照sort字段排序
                .sorted(Comparator.comparingInt(categoryEntity -> categoryEntity.getSort() == null?0:categoryEntity.getSort()))
                // 输出list
                .collect(Collectors.toList());
        return categoryEntityTree;
    }

    /**
     * 使用逻辑删除菜单通过类别id
     *
     * @param catIds 正如列表
     */

    @Override
    public void removeMenuByCatIds(List<Long> catIds) {
        //TODO  后期根据业务进行删除前的判断

        baseMapper.deleteBatchIds(catIds);
    }

    @Override
    public Long[] getCateLogIds(Long catelogId) {
        List<Long> cateLogIds = new ArrayList();
        cateLogIds = findParentPath(catelogId, cateLogIds);

        Collections.reverse(cateLogIds);

        return  cateLogIds.toArray(new Long[cateLogIds.size()]);
    }

    @Transactional
    @Override
    public void updateDetail(CategoryEntity category) {
        // 跟新基本信息
        this.updateById(category);
        //判断是否修改关联表的分类名称
        if (StringUtils.isNotEmpty(category.getName())){
            categoryBrandRelationDao.updateCateLogName(category.getCatId(), category.getName());
            //TODO 其他关联属性修改
        }


    }


    /**
     * 找到父路径
     *
     * @param catelogId  catelog id
     * @param cateLogIds 美食日志id
     * @return {@link List}<{@link Long}>
     */

    public List<Long> findParentPath(long catelogId, List<Long> cateLogIds){
        cateLogIds.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0){
            this.findParentPath(byId.getParentCid(),cateLogIds);
        }
        return cateLogIds;
    }

    /**
     * 设置子菜单
     *
     * @param currMenu 当前菜单
     * @param allMenus 所有菜单
     * @return {@link List}<{@link CategoryEntity}>
     */
    private List<CategoryEntity> setChildern(CategoryEntity currMenu, List<CategoryEntity> allMenus) {

        return allMenus.stream()
                .filter(item -> item.getParentCid().equals(currMenu.getCatId()))
                .map(menu -> {
                    menu.setChildren(setChildern(menu, allMenus));
                    return menu;
                })
                .sorted(Comparator.comparingInt(categoryEntity -> categoryEntity.getSort() == null?0:categoryEntity.getSort()))
                .collect(Collectors.toList());
    }

}