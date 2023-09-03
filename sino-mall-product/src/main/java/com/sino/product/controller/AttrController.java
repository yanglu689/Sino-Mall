package com.sino.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sino.product.entity.ProductAttrValueEntity;
import com.sino.product.vo.AttrRespVo;
import com.sino.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sino.product.service.AttrService;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.R;



/**
 * 商品属性
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:47:59
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

//    /product/attr/${type}/list/${this.catId}
    @GetMapping("{type}/list/{catId}")
    public R getBaseAttr(@RequestParam Map params, @PathVariable("catId") Long catId, @PathVariable("type") String type){
        PageUtils page = attrService.getBaseAttrPage(params, catId, type);
        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    public R info(@PathVariable("attrId") Long attrId){
//		AttrEntity attr = attrService.getById(attrId);

        AttrRespVo attrRespVo = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", attrRespVo);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrVo attr){
//		attrService.save(attr);

		attrService.saveAttr(attr);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrVo attrVo){
//		attrService.updateById(attr);
		attrService.updateDetail(attrVo);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

    /**
     * 列表product/attr/
     */
    @GetMapping("/base/listforspu/{spuId}")
    public R listForSpu(@PathVariable("spuId") Long spuId){
        List<ProductAttrValueEntity> entityList = attrService.listForSpu(spuId);

        return R.ok().put("data", entityList);
    }

    /**
     * 修改 /product/attr/update/{spuId}
     */
    @PostMapping("/update/{spuId}")
    public R updateBySpuId(@PathVariable("spuId") Long spuId, @RequestBody List<ProductAttrValueEntity> attrList){
        attrService.updateBySpuId(attrList,spuId);

        return R.ok();
    }



}
