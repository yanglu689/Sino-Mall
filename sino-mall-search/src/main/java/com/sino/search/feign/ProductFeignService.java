package com.sino.search.feign;

import com.sino.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("sino-mall-product")
public interface ProductFeignService {

    @GetMapping("/product/attr/info/{attrId}")
    R attrInfo(@PathVariable("attrId") Long attrId);

    @RequestMapping("/product/brand/infos/{brandIds}")
    R brandInfos( @PathVariable("brandIds") List<Long> brandIds);
}
