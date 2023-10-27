package com.sino.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.sino.common.utils.R;
import com.sino.product.entity.SkuImagesEntity;
import com.sino.product.entity.SpuInfoDescEntity;
import com.sino.product.feign.SeckillFeignService;
import com.sino.product.service.*;
import com.sino.product.vo.SecKillSkuRedisVo;
import com.sino.product.vo.SkuItemSaleAttrVo;
import com.sino.product.vo.SkuItemVo;
import com.sino.product.vo.SpuItemAttrGroupVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.Inflater;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.SkuInfoDao;
import com.sino.product.entity.SkuInfoEntity;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Autowired
    private SkuImagesService skuImagesService;

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private SeckillFeignService seckillFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 通过条件查询页面
     * key: '华为',//检索关键字
     * catelogId: 0,
     * brandId: 0,
     * min: 0,
     * max: 0
     *
     * @param params 参数个数
     * @return {@link PageUtils}
     */

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)) {
            wrapper.and(w -> {
                w.eq("sku_id", key).or().like("sku_name", key);
            });
        }

        String catalogId = (String) params.get("catelogId");
        if (StringUtils.isNotEmpty(catalogId) && !"0".equals(catalogId)) {
            BigDecimal bigDecimal = new BigDecimal(catalogId);
            if (bigDecimal.compareTo(new BigDecimal("0")) > 0) {
                wrapper.eq("catalog_id", catalogId);
            }
        }

        String brandId = (String) params.get("brandId");
        if (StringUtils.isNotEmpty(brandId) && !"0".equals(brandId)) {
            BigDecimal bigDecimal = new BigDecimal(brandId);
            if (bigDecimal.compareTo(new BigDecimal("0")) > 0) {
                wrapper.eq("brand_id", brandId);
            }
        }

        String min = (String) params.get("min");
        if (StringUtils.isNotEmpty(min)) {
            wrapper.ge("price", min);
        }
        String max = (String) params.get("max");
        if (StringUtils.isNotEmpty(max)) {
            BigDecimal bigDecimal = new BigDecimal(max);
            if (bigDecimal.compareTo(new BigDecimal("0")) > 0) {
                wrapper.le("price", max);
            }
        }


        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkuIdBySpuId(Long spuId) {
        List<SkuInfoEntity> skuInfoEntities = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id", spuId));
        return skuInfoEntities;
    }

    @Override
    public SkuItemVo item(Long skuId) throws ExecutionException, InterruptedException {
        SkuItemVo skuItemVo = new SkuItemVo();

        CompletableFuture<SkuInfoEntity> infoFuture = CompletableFuture.supplyAsync(() -> {
            // 1. sku基本信息获取 pms_sku_info
            SkuInfoEntity skuInfo = this.getById(skuId);
            skuItemVo.setInfo(skuInfo);
            return skuInfo;
        }, executor);

        CompletableFuture<Void> attrSaleFuture = infoFuture.thenAcceptAsync((res) -> {
            // 3. 获取spu的销售属性组合   pms_sku_sale_attr_value
            List<SkuItemSaleAttrVo> skuItemSaleAttrVos = skuSaleAttrValueService.getSkuSaleAttrBySpuId(res.getSpuId());
            skuItemVo.setSaleAttrs(skuItemSaleAttrVos);
        }, executor);

        CompletableFuture<Void> descFuture = infoFuture.thenAcceptAsync(res -> {
            // 4. 获取spu的介绍 pms_spu_info_desc
            SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(res.getSpuId());
            skuItemVo.setSpuDesc(spuInfoDesc);
        }, executor);

        CompletableFuture<Void> attrGroupFuture = infoFuture.thenAcceptAsync(res -> {
            // 5. 获取spu的规格参数信息
            List<SpuItemAttrGroupVo> spuItemAttrGroupVos = attrGroupService.getAttrGroupWithAttrBySpuId(res.getSpuId(), res.getCatalogId());
            skuItemVo.setGroupAttrs(spuItemAttrGroupVos);
        }, executor);

//        Long spuId = skuInfo.getSpuId();
//        Long catalogId = skuInfo.getCatalogId();

        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            // 2. sku的图片信息 pms_sku_images
            List<SkuImagesEntity> iamges = skuImagesService.getImagesBySkuId(skuId);
            skuItemVo.setImages(iamges);
        }, executor);

        CompletableFuture<Void> seckillFuture = CompletableFuture.runAsync(() -> {
            // 调用远程秒杀服务，查询当前商品是否参与秒杀活动
            R r = seckillFeignService.getSeckillInfo(skuId);
            if (r.getCode() == 0) {
                SecKillSkuRedisVo skuRedisVo = r.getData(new TypeReference<SecKillSkuRedisVo>() {
                });
                skuItemVo.setRedisVo(skuRedisVo);
            }
        }, executor);

        // 等待所有任务都完成
        CompletableFuture.allOf(attrSaleFuture,descFuture,attrGroupFuture,imageFuture, seckillFuture).get();

        return skuItemVo;
    }

}