package com.sino.product.fallback;

import com.sino.common.exception.BizCodeEnum;
import com.sino.common.utils.R;
import com.sino.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SeckillFeignServiceFallback implements SeckillFeignService {
    @Override
    public R getSeckillInfo(Long skuId) {
        log.info("降级getSeckillInfo ==>fallback");
        return R.error(BizCodeEnum.TO_MANY_REQUEST.getCode(), BizCodeEnum.TO_MANY_REQUEST.getMsg());
    }
}
