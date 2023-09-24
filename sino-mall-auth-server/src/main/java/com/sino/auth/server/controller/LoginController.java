package com.sino.auth.server.controller;

import com.alibaba.fastjson.TypeReference;
import com.sino.auth.server.config.AuthServerProperties;
import com.sino.auth.server.feign.MemberFeignService;
import com.sino.auth.server.feign.ThirdFeignService;
import com.sino.auth.server.vo.UserRegistVo;
import com.sino.auth.server.vo.UserloginVo;
import com.sino.common.constant.AuthServerConstant;
import com.sino.common.exception.BizCodeEnum;
import com.sino.common.utils.R;
import com.sino.common.vo.MemberRespVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

//    @GetMapping("/login.html")
//    public String login(){
//        return "login";
//    }
//
//    @GetMapping("/reg.html")
//    public String reg(){
//        return "reg";
//    }

    @Autowired
    private ThirdFeignService feignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private AuthServerProperties properties;

    @ResponseBody
    @GetMapping("/sms/sendcode")
    public R SendCode(@RequestParam("phone") String phone) {

        // TODO 接口防刷


        String cacheCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (StringUtils.isNotEmpty(cacheCode)) {
            String timeStr = cacheCode.split("_")[1];
            long time = Long.parseLong(timeStr);
            if (System.currentTimeMillis() - time < 60000) {
                // 60 秒内不能再发
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        // 验证码再次校验 key=phone value = code
        String code = generatedcode(6);
        String codeCache = code + "_" + System.currentTimeMillis();

        // redis缓存验证码， 防止同一个phone在60秒内发送验证码
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, codeCache, 10, TimeUnit.MINUTES);

        feignService.sendCode(phone, code);
        return R.ok();
    }


    /**
     * RedirectAttributes redirectAttributes 从定向携带数据
     * TODO 1 从定向携带数据，利用session原理，将数据放在session中，只要跳到下一个页面去除这个数据后，session里面的数据就会删掉
     * TODO 2 分布式下session问题
     *
     * @param vo
     * @param result
     * @param model
     * @param redirectAttributes
     * @return {@link String}
     */

    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, DefaultMessageSourceResolvable::getDefaultMessage));
            // model.addAttribute("errors",errors);
            redirectAttributes.addFlashAttribute("errors", errors);
            // forword:会使用本方法的请求发方法转发，可能请求方法会不一致，导致出错

            // 校验出错返回注册页
            return "redirect:http://"+properties.getAuthDomain()+"/reg.html";
        }

        // 真正的注册
        String code = vo.getCode();

        // 从redis取出验证码
        String cacheCode = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (StringUtils.isNotEmpty(cacheCode) && cacheCode.split("_")[0].equals(code)) {
            redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
            // 验证码验证成功， 调用远程服务进行注册
            R r = memberFeignService.regist(vo);
            if (r.getCode() != 0) {
                // 失败
                Map<String, String> errors = new HashMap<>();
                errors.put("msg", r.get("msg") != null ? r.get("msg").toString() : "");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://"+properties.getAuthDomain()+"/reg.html";
            }
        } else {
            Map<String, String> errors = new HashMap<>();
            errors.put("code", "验证码失效或验证失败");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://"+properties.getAuthDomain()+"/reg.html";
        }

        // 注册成功返回登录页面
        return "redirect:http://"+properties.getAuthDomain()+"/login.html";
    }

    @PostMapping("/login")
    public String login(UserloginVo userloginVo, RedirectAttributes redirectAttributes, HttpSession session){

        // 远程请求登录接口
        R r = memberFeignService.login(userloginVo);
        if (r.getCode() != 0){
            // 失败
            Map<String, String> errors = new HashMap<>();
            errors.put("msg", r.get("msg") != null ? r.get("msg").toString() : "");
            redirectAttributes.addFlashAttribute("errors", errors);
            return "redirect:http://"+properties.getAuthDomain()+"/login.html";
        }

        MemberRespVo data = r.getData("data", new TypeReference<MemberRespVo>() {
        });
        session.setAttribute(AuthServerConstant.LOGIN_USER, data);
        return "redirect:http://"+properties.getIndexDomain();
    }


    public String generatedcode(int count) {
        List<Integer> set = getRandomNumber(count);
        // 使用迭代器
        Iterator<Integer> iterator = set.iterator();
        // 临时记录数据
        String temp = "";
        while (iterator.hasNext()) {
            temp += iterator.next();

        }
        return temp;
    }

    public static List<Integer> getRandomNumber(int count) {
        // 使用SET以此保证写入的数据不重复
        List<Integer> set = new ArrayList<Integer>();
        // 随机数
        Random random = new Random();

        while (set.size() < count) {
            // nextInt返回一个伪随机数，它是取自此随机数生成器序列的、在 0（包括）
            // 和指定值（不包括）之间均匀分布的 int 值。
            set.add(random.nextInt(10));
        }
        return set;
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute == null){
            return "login";
        }else {
            return "redirect:http://"+properties.getIndexDomain();
        }
    }
}
