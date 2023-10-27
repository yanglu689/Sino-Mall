package com.sino.car.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sino.car.feign.ProductFeignService;
import com.sino.car.interceptor.CartInterceptor;
import com.sino.car.service.CartService;
import com.sino.car.vo.Cart;
import com.sino.car.vo.CartItem;
import com.sino.car.vo.SkuInfoVo;
import com.sino.car.vo.UserInfoTo;
import com.sino.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    private final String CART_PREFIX = "sinomall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = GetCartOps();

        // 查看商品是否存在
        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.hasText(res)) {
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        } else {
            // 2. 商品添加(新增)到购物车
            CartItem cartItem = new CartItem();
            CompletableFuture<Void> skuInfoTask = CompletableFuture.runAsync(() -> {
                // 1. 远程查询当前要添加的商品信息
                R info = productFeignService.skuInfo(skuId);
                if (info.getCode() == 0) {
                    SkuInfoVo skuInfo = info.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                    });
                    cartItem.setCheck(true);
                    cartItem.setCount(num);
                    cartItem.setImage(skuInfo.getSkuDefaultImg());
                    cartItem.setTitle(skuInfo.getSkuTitle());
                    cartItem.setSkuId(skuId);
                    cartItem.setPrice(skuInfo.getPrice());
                }
            }, executor);

            CompletableFuture<Void> saleAttrTask = CompletableFuture.runAsync(() -> {
                // 3. 远程查询sku的组合信息
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(skuSaleAttrValues);
            }, executor);

            CompletableFuture.allOf(skuInfoTask, saleAttrTask).get();

            String jsonString = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), jsonString);
            return cartItem;
        }
    }

    /**
     * 获取 SKU 信息
     *
     * @param skuId 货号
     * @return {@link CartItem}
     */
    @Override
    public CartItem getSkuInfo(String skuId) {
        BoundHashOperations<String, Object, Object> operations = GetCartOps();
        String str = (String) operations.get(skuId);
        CartItem cartItem = JSON.parseObject(str, CartItem.class);
        return cartItem;
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        Cart cart = new Cart();

        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() != null){
            // 已登录
            // 合并购物车
            String tempUserKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> tempCartItems = getCartItem(tempUserKey);
            if (!ObjectUtils.isEmpty(tempCartItems)){
                for (CartItem cartItem : tempCartItems) {
                    // 将临时购物车中的商品添加到当前用户购物车中
                    addToCart(cartItem.getSkuId(),cartItem.getCount());
                }
                // 清除临时购物车
                clearCart(tempUserKey);
            }

            // 查询当前用户中所有的购物项
            String userIdKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItem = getCartItem(userIdKey);
            cart.setItems(cartItem);
        }else {
            // 未登录
            String userKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItem> cartItems = getCartItem(userKey);
            if (cartItems !=null){
                cart.setItems(cartItems);
            }
        }
        return cart;
    }



    private List<CartItem> getCartItem(String userKey) {
        BoundHashOperations<String, Object, Object> cartOps = redisTemplate.boundHashOps(userKey);
        List<Object> values = cartOps.values();
        if (!ObjectUtils.isEmpty(values)){
            List<CartItem> cartItems = values.stream().map(obj -> {
                String carJson = obj.toString();
                CartItem cartItem = JSON.parseObject(carJson, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return cartItems;
        }

        return null;
    }

    /**
     * 绑定哈希操作:获取要操作的购物车
     *
     * @return {@link BoundHashOperations}<{@link String}, {@link Object}, {@link Object}>
     */
    private BoundHashOperations<String, Object, Object> GetCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartkey = "";
        if (userInfoTo.getUserId() != null) {
            cartkey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartkey = CART_PREFIX + userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> carOperations = redisTemplate.boundHashOps(cartkey);
        return carOperations;
    }

    @Override
    public void clearCart(String userKey) {
        redisTemplate.delete(userKey);
    }

    /**
     * 勾选购物项
     *
     * @param skuId 货号
     * @param check 检查
     */
    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = GetCartOps();
        CartItem skuInfo = getSkuInfo(skuId.toString());
        skuInfo.setCheck(check==1);
        String jsonString = JSON.toJSONString(skuInfo);
        cartOps.put(skuId.toString(), jsonString);
    }

    /**
     * 修改购物车商品数量
     *
     * @param skuId 货号
     * @param num   数字
     */
    @Override
    public void countItem(Long skuId, Integer num) {
        CartItem skuInfo = getSkuInfo(skuId.toString());
        skuInfo.setCount(num);
        String jsonString = JSON.toJSONString(skuInfo);
        BoundHashOperations<String, Object, Object> cartOps = GetCartOps();
        cartOps.put(skuId.toString(),jsonString);
    }

    /**
     * 删除购物车商品
     *
     * @param skuId 货号
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = GetCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public List<CartItem> getCurrentCart() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        if (userInfoTo.getUserId() !=null){
            String userKey = CART_PREFIX + userInfoTo.getUserId();
            List<CartItem> cartItem = getCartItem(userKey);
            cartItem = cartItem.stream().filter(CartItem::getCheck).map(item -> {
                Long skuId = item.getSkuId();
                BigDecimal price = productFeignService.getPrice(skuId);
                item.setPrice(price);
                return item;
            }).collect(Collectors.toList());

            return cartItem;
        }
        return null;
    }
}
