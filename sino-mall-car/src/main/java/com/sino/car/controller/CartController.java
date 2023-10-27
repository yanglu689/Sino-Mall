package com.sino.car.controller;

import com.sino.car.interceptor.CartInterceptor;
import com.sino.car.service.CartService;
import com.sino.car.vo.Cart;
import com.sino.car.vo.CartItem;
import com.sino.car.vo.SkuInfoVo;
import com.sino.car.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;


@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    /**
     * 购物车列表页面
     *
     * 浏览器有一个cookie，user-key:  标识用户身份，一个月后过期
     * 如果第一次使用jd的购物车功能，都会给一个临时的用户身份
     * 浏览器以后保存，每次访问都会带上这个cookie
     *
     * 登录：session有
     * 没登陆： 按照cookie里面带来的user-key来做、
     * 第一次：入股没有临时用户，帮忙创建一个临时用户
     *
     * @return {@link String}
     */
    @GetMapping("/cart.html")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        // UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Cart cart = cartService.getCart();
        model.addAttribute("cart", cart);
        return "cartList";
    }

    /**
     * 加入购物车
     *
     * @return {@link String}
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        CartItem cartItem= cartService.addToCart(skuId, num);

        redirectAttributes.addAttribute("skuId", skuId.toString());

        return "redirect:http://cart.sinomall.com/addToCartSuccess";
    }

    @GetMapping("/addToCartSuccess")
    public String addToCartSuccess(@RequestParam("skuId") String skuId, Model model){
        CartItem cartItem = cartService.getSkuInfo(skuId);
        model.addAttribute("item", cartItem);
        return "success";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId, @RequestParam("check") Integer check){
        cartService.checkItem(skuId, check);
        return "redirect:http://cart.sinomall.com/cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num){
        cartService.countItem(skuId, num);
        return "redirect:http://cart.sinomall.com/cart.html";
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){
        cartService.deleteItem(skuId);
        return "redirect:http://cart.sinomall.com/cart.html";
    }

    @GetMapping("/getCurrentUserCartItems")
    @ResponseBody
    public List<CartItem> getCurrentUserCartItems(){
        return cartService.getCurrentCart();
    }
}
