package com.sino.product.feign;

import com.sino.common.utils.R;
import com.sino.product.vo.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("sino-mall-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasStock")
    R getSkuHasStock(@RequestBody List<Long> skuIds);

}
