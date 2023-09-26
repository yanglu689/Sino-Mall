package com.sino.sso.client.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HelloController {

    @Value("${sso.server.url}")
    private String ssoServerUrl;

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){

        return "hello";
    }

    @GetMapping("/boss")
    public String employees(Model model, HttpSession session, @RequestParam(value = "token", required = false) String token){
        if (StringUtils.hasText(token)){

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> username = restTemplate.getForEntity("http://sso.com:8080/getuser?token="+token, String.class);
            session.setAttribute("loginUser", username.getBody());

        }

        if (session.getAttribute("loginUser") == null){
            // 没有登陆 前往登录页面
            // 登录成功使用redirect_url=http://client1.com/employees 跳转回来
            return "redirect:" + ssoServerUrl + "?redirect_url=http://client2.com:8082/boss";
        }
        List<String> users = new ArrayList<>();
        users.add("张三");
        users.add("李四");
        users.add("王五");
        model.addAttribute("emps", users);
        return "list";
    }
}
