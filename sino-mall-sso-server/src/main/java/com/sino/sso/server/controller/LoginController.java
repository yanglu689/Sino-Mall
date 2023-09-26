package com.sino.sso.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
public class LoginController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("/login")
    public String login(@RequestParam("username") String username,
                        @RequestParam("url") String url,
                        @RequestParam("password") String password,
                        HttpServletResponse response){
        if (StringUtils.hasText(username) && StringUtils.hasText(password)){
            // 登录成功
            String uuid = UUID.randomUUID().toString();
            Cookie ssotoken = new Cookie("ssotoken", uuid);
            stringRedisTemplate.opsForValue().set(uuid,username);
            response.addCookie(ssotoken);

            return "redirect:" + url +"?token="+uuid;
        }
        return "login";
    }

    @GetMapping("/login.html")
    public String loginPage(@RequestParam("redirect_url") String redirect_url, Model model, @CookieValue(value = "ssotoken", required = false) String ssoToken){
        if (StringUtils.hasText(ssoToken)){
            return "redirect:" + redirect_url +"?token="+ssoToken;
        }
        model.addAttribute("url", redirect_url);
        return "login";
    }

    @GetMapping("/getuser")
    @ResponseBody
    public String getUserByToken(@RequestParam("token") String token){

        String ssotoken = stringRedisTemplate.opsForValue().get(token);
        return ssotoken;
    }
}
