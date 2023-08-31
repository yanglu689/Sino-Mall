package com.sino.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.SpuImagesDao;
import com.sino.product.entity.SpuImagesEntity;
import com.sino.product.service.SpuImagesService;
import org.springframework.util.ObjectUtils;


@Service("spuImagesService")
public class SpuImagesServiceImpl extends ServiceImpl<SpuImagesDao, SpuImagesEntity> implements SpuImagesService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuImagesEntity> page = this.page(
                new Query<SpuImagesEntity>().getPage(params),
                new QueryWrapper<SpuImagesEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存spu图片
     *
     * @param spuId  spu id
     * @param images 图片
     */

    @Override
    public void savaImages(Long spuId, List<String> images) {
        if (!ObjectUtils.isEmpty(images)){
            List<SpuImagesEntity> imageCollect = images.stream().map(url -> {
                SpuImagesEntity imagesEntity = new SpuImagesEntity();
                imagesEntity.setSpuId(spuId);
                imagesEntity.setImgUrl(url);
                return imagesEntity;
            }).collect(Collectors.toList());
            this.saveBatch(imageCollect);
        }
    }

}