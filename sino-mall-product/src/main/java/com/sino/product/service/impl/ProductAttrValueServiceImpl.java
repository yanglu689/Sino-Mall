package com.sino.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.ProductAttrValueDao;
import com.sino.product.entity.ProductAttrValueEntity;
import com.sino.product.service.ProductAttrValueService;
import org.springframework.util.ObjectUtils;


@Service("productAttrValueService")
public class ProductAttrValueServiceImpl extends ServiceImpl<ProductAttrValueDao, ProductAttrValueEntity> implements ProductAttrValueService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<ProductAttrValueEntity> page = this.page(
                new Query<ProductAttrValueEntity>().getPage(params),
                new QueryWrapper<ProductAttrValueEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void updateSpuAttr(List<ProductAttrValueEntity> attrList, Long spuId) {
        if (ObjectUtils.isEmpty(attrList)){
            return;
        }
        this.baseMapper.delete(new QueryWrapper<ProductAttrValueEntity>().eq("spu_id", spuId ));

        attrList.stream().forEach(item -> item.setSpuId(spuId));

        this.saveBatch(attrList);
    }

}