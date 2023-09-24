package com.sino.auth.server.feign;

import com.sino.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("sino-mall-third-party")
public interface ThirdFeignService {

    @GetMapping("/sms/sendcode")
    R sendCode(@RequestParam("phone") String phone, @RequestParam("code") String code);

}
