package com.sino.coupon.service.impl;

import com.sino.common.to.MemberPrice;
import com.sino.common.to.SkuReductionTo;
import com.sino.coupon.entity.MemberPriceEntity;
import com.sino.coupon.entity.SkuLadderEntity;
import com.sino.coupon.service.MemberPriceService;
import com.sino.coupon.service.SkuLadderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.coupon.dao.SkuFullReductionDao;
import com.sino.coupon.entity.SkuFullReductionEntity;
import com.sino.coupon.service.SkuFullReductionService;
import org.springframework.util.ObjectUtils;


@Service("skuFullReductionService")
public class SkuFullReductionServiceImpl extends ServiceImpl<SkuFullReductionDao, SkuFullReductionEntity> implements SkuFullReductionService {

    @Autowired
    private SkuLadderService skuLadderService;

    @Autowired
    private MemberPriceService memberPriceService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuFullReductionEntity> page = this.page(
                new Query<SkuFullReductionEntity>().getPage(params),
                new QueryWrapper<SkuFullReductionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuReduction(SkuReductionTo skuReductionTo) {
        // 6.4 保存sku的优惠、满减等信息-》 sms_sku_ladder、sms_sku_full_reduction、sms_member_price
        // 1. 保存阶梯价格 sms_sku_ladder
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        skuLadderEntity.setSkuId(skuReductionTo.getSkuId());
        skuLadderEntity.setFullCount(skuReductionTo.getFullCount());
        skuLadderEntity.setDiscount(skuReductionTo.getDiscount());
        skuLadderEntity.setAddOther(skuReductionTo.getCountStatus());
        if (skuReductionTo.getFullCount() > 0) {
            skuLadderService.save(skuLadderEntity);
        }

        // 2. 保存满减信息 sms_sku_full_reduction
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuReductionTo, skuFullReductionEntity);
        if (skuFullReductionEntity.getFullPrice().compareTo(new BigDecimal("0")) == 1) {
            this.save(skuFullReductionEntity);
        }

        // 3. 保存会员价格sms_member_price
        List<MemberPrice> memberPrice = skuReductionTo.getMemberPrice();
        if (!ObjectUtils.isEmpty(memberPrice)) {
            List<MemberPriceEntity> memberPriceEntities = memberPrice.stream()
                    .filter(item -> item.getPrice().compareTo(new BigDecimal("0")) > 0)
                    .map(mem -> {
                        MemberPriceEntity memberPriceEntity = new MemberPriceEntity();
                        memberPriceEntity.setSkuId(skuReductionTo.getSkuId());
                        memberPriceEntity.setMemberLevelId(mem.getId());
                        memberPriceEntity.setMemberLevelName(mem.getName());
                        memberPriceEntity.setMemberPrice(mem.getPrice());
                        memberPriceEntity.setAddOther(1);
                        return memberPriceEntity;
                    }).collect(Collectors.toList());
            memberPriceService.saveBatch(memberPriceEntities);
        }

    }


}