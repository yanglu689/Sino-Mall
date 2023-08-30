package com.sino.member.feign;

import com.sino.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("sino-mall-coupon")
public interface CouponFeignService {
    @RequestMapping("/coupon/coupon/member/coupon")
    public R memberCoupon();
}
