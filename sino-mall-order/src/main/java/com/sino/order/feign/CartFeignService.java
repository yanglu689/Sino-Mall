package com.sino.order.feign;

import com.sino.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("sino-mall-car")
public interface CartFeignService {

    @GetMapping("/getCurrentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItems();
}
