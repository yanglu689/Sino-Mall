package com.sino.ware.controller;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.sino.ware.vo.MergeVo;
import com.sino.ware.vo.PurchaseDoneVo;
import net.sf.jsqlparser.statement.merge.Merge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sino.ware.entity.PurchaseEntity;
import com.sino.ware.service.PurchaseService;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.R;


/**
 * 采购信息
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 15:23:49
 */
@RestController
@RequestMapping("ware/purchase")
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;

    /**
     * 查询未领取采购订单列表
     * /ware/purchase/unreceive/list
     */
    @RequestMapping("/unreceive/list")
    public R unreceiveList(@RequestParam Map<String, Object> params) {
        PageUtils page = purchaseService.queryUnreceiveListPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = purchaseService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        PurchaseEntity purchase = purchaseService.getById(id);

        return R.ok().put("purchase", purchase);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody PurchaseEntity purchase) {
        purchase.setCreateTime(new Date());
        purchase.setUpdateTime(new Date());
        purchaseService.save(purchase);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody PurchaseEntity purchase) {
        purchaseService.updateById(purchase);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        purchaseService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * ware/purchase/merge
     * 合并采购需求到采购订单
     * @param mergeVo
     * @return {@link R}
     */
    @PostMapping("merge")
    public R merge(@RequestBody MergeVo mergeVo) {
        purchaseService.mergePurchase(mergeVo);
        return R.ok();
    }

    /**
     * 领取采购订单
     * ware/purchase/received
     * @param purchaseIds
     * @return {@link R}
     */
    @PostMapping("/received")
    public R receivedPurchase(@RequestBody List<Long> purchaseIds){
        purchaseService.receivedPurchase(purchaseIds);
        return R.ok();
    }

    /**
     * 完成采购订单
     * ware/purchase/done
     * @param purchaseDoneVo
     * @return {@link R}
     */
    @PostMapping("/done")
    public R purchaseDone(@RequestBody PurchaseDoneVo purchaseDoneVo){
        purchaseService.purchaseDone(purchaseDoneVo);
        return R.ok();
    }



}
