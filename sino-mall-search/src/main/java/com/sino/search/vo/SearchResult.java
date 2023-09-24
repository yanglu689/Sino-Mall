package com.sino.search.vo;

import com.sino.common.to.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResult {
    /**
     * 查询所有商品的信息
     */
    private List<SkuEsModel> products;

    /**
     * 一下是分页信息
     */
    private Integer pageNum;//当前页码
    private Long total;//总记录数
    private Integer totalPages; //总页码
    private List<Integer> pageNavs;

    private List<BrandVo> brands; //当前查询道德结果，所有涉及到的所有品牌
    private List<CatalogVo> catalogs; //当前查询道德结果，所有涉及到的所有分类
    private List<AttrVo> attrs; //当前查询道德结果，所有涉及到的所有属性

    // 面包屑导航
    private List<NavVo> navs = new ArrayList<>();
    private List<Long> attrIds = new ArrayList<>();

    @Data
    public static class NavVo{
        private String navName;
        private String navValue;
        private String link;
    }

    @Data
    public static class BrandVo{
        /**
         * 品牌id
         */
        private Long brandId;
        /**
         * 品牌名称
         */
        private String brandName;
        /**
         * 品牌loge
         */
        private String brandImg;
    }

    @Data
    public static class AttrVo{
        private Long attrId;

        private String attrName;

        private List<String> attrValue;
    }

    @Data
    public static class CatalogVo{
        /**
         * 类别id
         */
        private Long catalogId;
        /**
         * 类别名称
         */
        private String catalogName;
    }


}
