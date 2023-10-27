package com.sino.car.service;


import com.sino.car.vo.Cart;
import com.sino.car.vo.CartItem;
import com.sino.car.vo.SkuInfoVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItem getSkuInfo(String skuId);

    Cart getCart() throws ExecutionException, InterruptedException;

    void clearCart(String userKey);

    void checkItem(Long skuId, Integer check);

    void countItem(Long skuId, Integer num);

    void deleteItem(Long skuId);

    List<CartItem> getCurrentCart();
}
