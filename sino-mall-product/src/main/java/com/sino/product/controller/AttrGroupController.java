package com.sino.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sino.product.entity.AttrEntity;
import com.sino.product.service.CategoryService;
import com.sino.product.vo.AttrAttrGroupRelationVo;
import com.sino.product.vo.AttrGroupVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sino.product.entity.AttrGroupEntity;
import com.sino.product.service.AttrGroupService;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.R;


/**
 * 属性分组
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 12:47:59
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 列表
     */
    @RequestMapping("/list/{catelogId}")
    public R list(@RequestParam Map<String, Object> params, @PathVariable("catelogId") Long catelogId) {
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params, catelogId);
        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId) {
        AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        //根据三级分类的id查询分类的路径
        Long[] catelogPath = categoryService.getCateLogIds(attrGroup.getCatelogId());
        attrGroup.setCatelogPath(catelogPath);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup) {
        attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds) {
        attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

    //    "/product/attrgroup/" + this.attrGroupId + "/attr/relation"
    @GetMapping("/{attrGroupId}/attr/relation")
    public R attrGroupRelationList(@PathVariable("attrGroupId") Long attrGroupId) {
        List<AttrEntity> attrList = attrGroupService.getAttrListByAttrGroupId(attrGroupId);
        return R.ok().put("data", attrList);
    }

    //http://localhost:88/api/product/attrgroup/attr/relation/delete
    @PostMapping("/attr/relation/delete")
    public R deleteRelation(@RequestBody AttrAttrGroupRelationVo[] relationVo) {
        attrGroupService.deleteRelation(relationVo);
        return R.ok();
    }

//    http://localhost:88/api/product/attrgroup/1/noattr/relation?t=1693380627792&page=1&limit=10&key=
    @GetMapping("/{attrGroupId}/noattr/relation")
    public R findCanBindAttrList(@RequestParam Map params, @PathVariable("attrGroupId") Long attrGroupId){
        PageUtils page = attrGroupService.findCanBindAttrList(params, attrGroupId);

        return R.ok().put("page",page);
    }

//    http://localhost:88/api/product/attrgroup/attr/relation
    @PostMapping("/attr/relation")
    public R addAttrAttrGroup(@RequestBody List<AttrAttrGroupRelationVo> vo){
        attrGroupService.addAttrAttrGroup(vo);
        return R.ok();
    }
//    http://localhost:88/api/product/attrgroup/171/withattr?t=1693403462529
    @GetMapping("/{catelogId}/withattr")
    public R getAttrGroupWithAttr(@PathVariable("catelogId") Long catelogId){

        List<AttrGroupVo> attrGroupVos = attrGroupService.getAttrGroupWithAttr(catelogId);

        return R.ok().put("data",attrGroupVos);
    }
}
