package com.sino.coupon.dao;

import com.sino.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 14:21:03
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
