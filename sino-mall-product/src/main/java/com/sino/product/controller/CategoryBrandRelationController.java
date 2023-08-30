package com.sino.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sino.product.entity.BrandEntity;
import com.sino.product.vo.BrandRespVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sino.product.entity.CategoryBrandRelationEntity;
import com.sino.product.service.CategoryBrandRelationService;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.R;



/**
 * 品牌分类关联
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:47:59
 */
@RestController
@RequestMapping("product/categorybrandrelation")
public class CategoryBrandRelationController {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = categoryBrandRelationService.queryPage(params);

        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @GetMapping("/catelog/list")
    public R catelogList(@RequestParam("brandId") Long brandId){
        List<CategoryBrandRelationEntity> data = categoryBrandRelationService.list(new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_Id",brandId));

        return R.ok().put("data", data);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok().put("categoryBrandRelation", categoryBrandRelation);
    }

    /**
     * 保存
     * brandId:this.brandId,catelogId:this.catelogPath[this.catelogPath.length-1]}
     */
    @RequestMapping("/save")
    public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
//		categoryBrandRelationService.save(categoryBrandRelation);
        categoryBrandRelationService.saveDetail(categoryBrandRelation);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.updateById(categoryBrandRelation);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    /**
     * 被猫品牌标识
     *http://localhost:88/api/product/categorybrandrelation/brands/list?t=1693395219797&catId=165
     * Controller
     * 1 接收校验前端传来的参数
     * 2 传递参数给service，并接收返回值
     * 3 处理service返回值，并返回给前端页面
     * @param catId 猫id
     * @return {@link R}
     */
    @GetMapping("/brands/list")
    public R getBrandsByCatId(@RequestParam("catId") Long catId){
        // 使用实体类接受参数
        List<BrandEntity> entityList = categoryBrandRelationService.getbrandsByCatId(catId);
        List<BrandRespVo> vos = entityList.stream().map(entity -> {
            BrandRespVo vo = new BrandRespVo();
            vo.setBrandId(entity.getBrandId());
            vo.setBrandName(entity.getName());
            return vo;
        }).collect(Collectors.toList());
        return R.ok().put("data",vos);
    }

}
