package com.sino.product.service.impl;

import com.sino.common.to.SkuReductionTo;
import com.sino.common.to.SpuBoundsTo;
import com.sino.common.utils.R;
import com.sino.product.entity.*;
import com.sino.product.feign.CouponFeignService;
import com.sino.product.service.*;
import com.sino.product.vo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    private SpuInfoDescService spuInfoDescService;

    @Autowired
    private SpuImagesService spuImagesService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private ProductAttrValueService productAttrValueService;

    @Autowired
    private  SkuInfoService skuInfoService;

    @Autowired
    private  SkuImagesService skuImagesService;

    @Autowired
    private SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    private CouponFeignService couponFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 保存spu信息
     * TODO 高级部分完善保存失败 事务同步问题
     *
     * @param spuSaveVo spu节省签证官
     */

    @Override
    @Transactional
    public void savaSpuInfo(SpuSaveVo spuSaveVo) {
        // 1. 保存spu的基本信息-》 pms_spu_info
        SpuInfoEntity spuInfoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(spuSaveVo, spuInfoEntity);
        spuInfoEntity.setCreateTime(new Date());
        spuInfoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(spuInfoEntity);

        Long spuId = spuInfoEntity.getId();

        // 2. 保存spu的描述信息-》 pms_spu_info_desc
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(spuId);
        spuInfoDescEntity.setDecript(String.join(",", spuSaveVo.getDecript()));
        spuInfoDescService.savaSpuInfoDesc(spuInfoDescEntity);

        // 3. 保存spu的图片集 -》 pms_spu_images
        List<String> images = spuSaveVo.getImages();
        spuImagesService.savaImages(spuId, images);

        // 4. 保存spu额规格参数-》 pms_product_attr_value
        List<BaseAttrs> baseAttrs = spuSaveVo.getBaseAttrs();
        List<ProductAttrValueEntity> productAttrValueEntities = baseAttrs.stream().map(attr -> {
            ProductAttrValueEntity productAttrValueEntity = new ProductAttrValueEntity();
            productAttrValueEntity.setSpuId(spuId);
            productAttrValueEntity.setAttrId(attr.getAttrId());
            AttrEntity attrEntity = attrService.getById(attr.getAttrId());
            if (!ObjectUtils.isEmpty(attrEntity)) {
                productAttrValueEntity.setAttrName(attrEntity.getAttrName());
            }
            productAttrValueEntity.setAttrValue(attr.getAttrValues());
            return productAttrValueEntity;
        }).collect(Collectors.toList());
        productAttrValueService.saveBatch(productAttrValueEntities);

        // 5. 保存spu的积分信息 -》 sms _spu_bounds
        Bounds bounds = spuSaveVo.getBounds();
        SpuBoundsTo spuBoundsTo = new SpuBoundsTo();
        BeanUtils.copyProperties(bounds, spuBoundsTo);
        spuBoundsTo.setSpuId(spuId);
        R r1 = couponFeignService.saveSkuBounds(spuBoundsTo);
        if (r1.getCode() != 0){
            log.error("远程保存积分信息失败");
        }

        // 6. 保存spu对应的所有sku信息
        List<Skus> skus = spuSaveVo.getSkus();
        skus.forEach(sku -> {
            boolean imgIsNull = ObjectUtils.isEmpty(sku.getImages());
            String img = "";
            if (!imgIsNull){
                for (Images image : sku.getImages()) {
                    if (image.getDefaultImg() == 1){
                        img = image.getImgUrl();
                    }
                }
            }

            SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
            BeanUtils.copyProperties(sku, skuInfoEntity);
            skuInfoEntity.setSpuId(spuId);
            skuInfoEntity.setBrandId(spuSaveVo.getBrandId());
            skuInfoEntity.setCatalogId(spuSaveVo.getCatalogId());
            skuInfoEntity.setSaleCount(0L);
            skuInfoEntity.setSkuDefaultImg(img);
            // 6.1 保存sku的基本信息 -》 pms_sku_info
            skuInfoService.save(skuInfoEntity);

            Long skuId = skuInfoEntity.getSkuId();

            if (!imgIsNull) {
                List<SkuImagesEntity> skuImagesEntities = sku.getImages()
                        .stream()
                        .filter(item -> StringUtils.isNotEmpty(item.getImgUrl()))
                        .map(imge -> {
                            SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                            skuImagesEntity.setSkuId(skuId);
                            skuImagesEntity.setDefaultImg(imge.getDefaultImg());
                            skuImagesEntity.setImgUrl(imge.getImgUrl());
                            return skuImagesEntity;
                        }).collect(Collectors.toList());
                // 6.2 保存sku的图片信息 -》 pms_sku_images
                // TODOOK 没有图片的无需保存
                skuImagesService.saveBatch(skuImagesEntities);
            }

            // 6.3 保存sku的销售属性信息 -》 pms_sku_sale_attr_value
            List<Attr> attr = sku.getAttr();
            List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attr.stream().map(a -> {
                SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                BeanUtils.copyProperties(a, skuSaleAttrValueEntity);
                skuSaleAttrValueEntity.setSkuId(skuId);
                return skuSaleAttrValueEntity;
            }).collect(Collectors.toList());
            skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);
            // 6.4 保存sku的优惠、满减等信息-》 sms_sku_ladder、sms_sku_full_reduction、sms_member_price
            SkuReductionTo skuReductionTo = new SkuReductionTo();
            BeanUtils.copyProperties(sku, skuReductionTo);
            skuReductionTo.setSkuId(skuId);
            if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) > 0){
                R r = couponFeignService.saveSkuReduction(skuReductionTo);
                if (r.getCode() != 0){
                    log.error("远程保存sku优惠信息失败");
                }
            }
        });

    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity spuInfoEntity) {
        this.save(spuInfoEntity);
    }

    /**
     * 通过条件查询页面
     *   key: '华为',//检索关键字
     *    catelogId: 6,//三级分类id
     *    brandId: 1,//品牌id
     *    status: 0,//商品状
     * @param params 参数个数
     * @return {@link PageUtils}
     */

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)){
            wrapper.and(w -> {
                w.eq("id", key).or().like("spu_name", key);
            });
        }

        String catelogId = (String) params.get("catelogId");
        if (StringUtils.isNotEmpty(catelogId) && !"0".equals(catelogId)){
            BigDecimal bigDecimal = new BigDecimal(catelogId);
            if (bigDecimal.compareTo(new BigDecimal("0")) > 0) {
                wrapper.eq("catalog_id", catelogId);
            }
        }

        String brandId = (String) params.get("brandId");
        if (StringUtils.isNotEmpty(brandId) && !"0".equals(brandId)) {
            BigDecimal bigDecimal = new BigDecimal(brandId);
            if (bigDecimal.compareTo(new BigDecimal("0")) > 0) {
                wrapper.eq("brand_id", brandId);
            }
        }

        String status = (String) params.get("status");
        if (StringUtils.isNotEmpty(status)){
            wrapper.eq("publish_status",status);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

}