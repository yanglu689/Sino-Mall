package com.sino.auth.server.feign;

import com.sino.auth.server.vo.SocialUser;
import com.sino.auth.server.vo.UserRegistVo;
import com.sino.auth.server.vo.UserloginVo;
import com.sino.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("sino-mall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegistVo registVo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserloginVo memberLoginVo);

    @PostMapping("/member/member/oauth2/login")
    R oauthLogin(@RequestBody SocialUser socialUser);
}
