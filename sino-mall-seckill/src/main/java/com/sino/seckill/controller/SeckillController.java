package com.sino.seckill.controller;

import com.sino.common.utils.R;
import com.sino.seckill.service.SeckillService;
import com.sino.seckill.to.SecKillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/currentSeckillSkus")
    @ResponseBody
    public R getCurrentSeckillSkus(){

        List<SecKillSkuRedisTo> skuTos = seckillService.getCurrentSeckillSkus();
        return R.ok().setData(skuTos);
    }

    @GetMapping("/sku/seckill")
    @ResponseBody
    public R getSeckillInfo(@RequestParam Long skuId){
        SecKillSkuRedisTo skuTo = seckillService.getSeckillInfo(skuId);
        return R.ok().setData(skuTo);
    }

    @GetMapping("/kill")
    public String kill(@RequestParam("killId") String killId, @RequestParam("key") String key, @RequestParam("num") Integer num, Model model){
        // 用户登录后才可以进行秒杀
        String orderSn = seckillService.kill(killId, key, num);
        model.addAttribute("orderSn", orderSn);
        return "success";
    }
}
