package com.sino.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 *  catalog3Id=165&keyword=?&sort=saleCount_asc/desc&hasStock=0/1
 * @author yanglupc
 * @date 2023/09/13
 */

@Data
public class SearchParam {
    private String keyword; //页面传过来的全文匹配关键字
    private Long catalog3Id; //三级分类

    /**
     * 排序
     * sort=saleCount_asc/desc
     * sort=skuPrice_asc/desc
     * sort=hotScore_asc/desc
     */
    private String sort; //排序

    /**
     * 好多的过滤条件
     * hasStock(是否有货) skuPrice区间 brandId catalog3Id attrs
     * hasStock=0/1
     * skuPrice=1_500/_500/500_
     * attrs2_5寸：6寸
     *
     */
    private Integer hasStock; //是否只显示有货 0 无  1 有
    private String skuPrice; // 价格区间
    private List<Long> brandId; // 品牌
    private List<String> attrs; // 按照属性进行查询，可以多选
    private Integer pageNum = 1; //页码

    private String _queryString; //原生的所有查询条件



}
