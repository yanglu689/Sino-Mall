package com.sino.product.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sino.common.validator.group.AddGroup;
import com.sino.common.validator.group.UpdateGroup;
import com.sino.common.validator.group.UpdateStatusGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sino.product.entity.BrandEntity;
import com.sino.product.service.BrandService;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.R;


/**
 * 品牌
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:47:59
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    public R info(@PathVariable("brandId") Long brandId){
		BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@Validated(AddGroup.class) @RequestBody BrandEntity brand/*, BindingResult result*/){
//        Map map = new HashMap();
//        if (result.hasErrors()){
//            result.getFieldErrors().forEach((item) -> {
//                String errMsg =item.getDefaultMessage();
//                String field = item.getField();
//                map.put(field,errMsg);
//            });
//            return R.error(400,"提交的数据不合法").put("data", map);
//        }else {
//
//        }
        brandService.save(brand);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand){
//		brandService.updateById(brand);
		brandService.updateDetailById(brand);

        return R.ok();
    }

    /**
     * 修改状态
     */
    @RequestMapping("/update/status")
    public R updateStatusGroup(@Validated(UpdateStatusGroup.class) @RequestBody BrandEntity brand){
        brandService.updateById(brand);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] brandIds){
		brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

    /**
     * 信息
     */
    @RequestMapping("/infos/{brandIds}")
    public R infos( @PathVariable("brandIds") List<Long> brandIds){
        List<BrandEntity> brands = brandService.getBrandInfos(brandIds);

        return R.ok().put("brands", brands);
    }
}
