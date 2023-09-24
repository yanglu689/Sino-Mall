package com.sino.auth.server.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.sino.auth.server.config.AuthServerProperties;
import com.sino.auth.server.feign.MemberFeignService;
import com.sino.auth.server.utils.HttpUtils;
import com.sino.common.constant.AuthServerConstant;
import com.sino.common.vo.MemberRespVo;
import com.sino.auth.server.vo.SocialUser;
import com.sino.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * OAuth 社交登录接口
 *
 * @author yanglupc
 * @date 2023/09/24
 */

@Slf4j
@Controller
public class OAuth2Controller {

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private AuthServerProperties properties;

    @GetMapping("/oauth2.0/gitee/success")
    public String gitee(@RequestParam("code") String code, HttpSession httpSession) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("grant_type", "authorization_code");
        map.put("code", code);
        map.put("client_id", "3d407bdcbf4e13def4fbd424790b63b9ffb919964c48488ed6c4a7e25953de96");
        map.put("redirect_uri", "http://"+properties.getAuthDomain()+"/oauth2.0/gitee/success");
        map.put("client_secret", "a10b901485ccc0f44f77bf874afe8bf63e161c531e8109a0384835a4711faa47");

        // 1.换取access_token
        HttpResponse response = HttpUtils.doPost("https://gitee.com", "/oauth/token", "post", new HashMap<>(), null, map);

        // 2.处理响应数据
        if (response.getStatusLine().getStatusCode() == 200) {
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            // 获取用户的唯一id
            setSocialUid(socialUser);
            // 知道是哪个社交用户
            // 判断用户是不是第一次进入这个网站，如果是第一次，先进行注册（自动生成一个用户信息，以后这个用户就对应这个用户西悉尼）
            // 登录或者注册社交帐号
            R r = memberFeignService.oauthLogin(socialUser);
            if (r.getCode() == 0) {
                MemberRespVo data = r.getData(new TypeReference<MemberRespVo>() {
                });

                // 1. 第一次使用session； 命令浏览器保存卡号，JSESSIONID这个cookie；
                // 以后浏览器访问那个网站就会带上这个网站的cookie
                // 子域之间：sinomall.com auth.sinomall.com  order.sinomall.com
                // 发卡的时候（指定域名作为父域名） 即使是子系统发的卡，也能让父域直接使用
                // TODO 1、默认发的令牌，session=sfdlahjf,  作用域是当前域  （解决子域session共享问题）
                // TODO 2、使用json的序列化方式来序列化对象数据到redis中
                httpSession.setAttribute(AuthServerConstant.LOGIN_USER,data);

                log.info("登录成功，用户信息:{}",data);
                return "redirect:http://"+properties.getIndexDomain();
            }
        }

        return "redirect:http://"+properties.getAuthDomain()+"/login.html";
    }

    private void setSocialUid(SocialUser socialUser) throws Exception {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("access_token", socialUser.getAccess_token());
        // 调用gitee查询用户信息接口
        HttpResponse infoResp = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", new HashMap<>(), queryMap);
        if (infoResp.getStatusLine().getStatusCode() == 200) {
            String infoJson = EntityUtils.toString(infoResp.getEntity());
            JSONObject jsonObject = JSON.parseObject(infoJson);
            // 获取用户的id
            String uid = jsonObject.get("id").toString();
            socialUser.setUid(uid);

            Object name = jsonObject.get("name");

            if (name != null) {
                socialUser.setName(name.toString());
            }

            Object email = jsonObject.get("email");
            if (email != null) {
                socialUser.setEmail(email.toString());
            }
        }
    }
}
