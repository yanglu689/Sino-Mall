package com.sino.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sino.common.to.SkuEsModel;
import com.sino.common.utils.R;
import com.sino.search.config.SinoMallElasticSearchConfig;
import com.sino.search.constant.EsConstant;
import com.sino.search.feign.ProductFeignService;
import com.sino.search.service.MallSearchService;
import com.sino.search.vo.AttrResponseVo;
import com.sino.search.vo.BrandRespVo;
import com.sino.search.vo.SearchParam;
import com.sino.search.vo.SearchResult;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ProductFeignService productFeignService;

    /**
     * # 模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存），排序，分页，高亮， 聚合分析？
     */
    @Override
    public SearchResult search(SearchParam param) {
        SearchResult searchResult = null;
        // 动态构建出查询需要的DSL
        SearchRequest searchRequest = searchBuilder(param);

        try {
            // 使用高阶客户端发送es查询请求，得到响应数据
            SearchResponse searchResponse = client.search(searchRequest, SinoMallElasticSearchConfig.COMMON_OPTIONS);

            searchResult = responseBuilder(searchResponse, param);
            return searchResult;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchResult;
    }

    /**
     * 通过es查询结果封装响应数据
     *
     * @param searchResponse
     * @return {@link SearchResult}
     */

    private SearchResult responseBuilder(SearchResponse searchResponse, SearchParam param) {
        SearchResult result = new SearchResult();

        SearchHits hits = searchResponse.getHits();

        // 0. 设置返回的商品集合
        List<SkuEsModel> products = new ArrayList<>();
        SearchHit[] hitArr = hits.getHits();
        if (!ObjectUtils.isEmpty(hitArr)) {
            // 遍历命中的每一个商品
            for (SearchHit hit : hitArr) {
                String sourceAsString = hit.getSourceAsString();
                // json反序列化
                SkuEsModel skuEsModel = JSON.parseObject(sourceAsString, SkuEsModel.class);
                if (StringUtils.isNotEmpty(param.getKeyword())) {
                    HighlightField highlightField = hit.getHighlightFields().get("skuTitle");
                    String skuTitle = highlightField.getFragments()[0].string();
                    skuEsModel.setSkuTitle(skuTitle);
                }
                products.add(skuEsModel);
            }
        }
        result.setProducts(products);

        // 获取最外层的聚合
        Aggregations aggregations = searchResponse.getAggregations();

        // 1. 设置品牌集合
        List<SearchResult.BrandVo> brands = new ArrayList<>();
        // 获取品牌聚合
        ParsedLongTerms brand = aggregations.get("brand_agg");
        List<? extends Terms.Bucket> brandBuckets = brand.getBuckets();
        for (Terms.Bucket bucket : brandBuckets) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            String brand_agg = bucket.getKeyAsString();
            Long brandId = Long.parseLong(brand_agg);
            // 获得品牌聚合里的名字的聚合
            String brandName = ((ParsedStringTerms) bucket.getAggregations().get("brand_name_agg")).getBuckets().get(0).getKeyAsString();
            // 获得品牌聚合里的图片的聚合
            String brandImg = ((ParsedStringTerms) bucket.getAggregations().get("brand_img_agg")).getBuckets().get(0).getKeyAsString();
            brandVo.setBrandId(brandId);
            brandVo.setBrandName(brandName);
            brandVo.setBrandImg(brandImg);
            brands.add(brandVo);
        }
        result.setBrands(brands);

        // 2. 设置分类集合
        List<SearchResult.CatalogVo> catalogs = new ArrayList<>();
        // 获取品牌聚合
        ParsedLongTerms catalog = aggregations.get("catalog_agg");
        List<? extends Terms.Bucket> catalogBuckets = catalog.getBuckets();
        for (Terms.Bucket bucket : catalogBuckets) {
            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            String catalogKey = bucket.getKeyAsString();
            long cataLogId = Long.parseLong(catalogKey);
            ParsedStringTerms parsedStringTerms = bucket.getAggregations().get("catalog_name");
            String cataLogName = "";
            if (!ObjectUtils.isEmpty(parsedStringTerms) && !ObjectUtils.isEmpty(parsedStringTerms.getBuckets())) {
                cataLogName = parsedStringTerms.getBuckets().get(0).getKeyAsString();
            }
            catalogVo.setCatalogId(cataLogId);
            catalogVo.setCatalogName(cataLogName);
            catalogs.add(catalogVo);
        }
        result.setCatalogs(catalogs);

        // 3. 设置属性集合
        List<SearchResult.AttrVo> attrs = new ArrayList<>();
        // 根据attr_agg获取嵌套的集合
        ParsedNested attrNested = searchResponse.getAggregations().get("attr_agg");
        // 根据attr_id_agg获取attr属性集合
        ParsedLongTerms attrIdAgg = attrNested.getAggregations().get("attr_id_agg");
        List<? extends Terms.Bucket> attrBuckets = attrIdAgg.getBuckets();
        for (Terms.Bucket bucket : attrBuckets) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            String attrKey = bucket.getKeyAsString();
            long attrId = Long.parseLong(attrKey);
            // 通过attr_name_agg获取属性名
            String attrName = ((ParsedStringTerms) bucket.getAggregations().get("attr_name_agg")).getBuckets().get(0).getKeyAsString();
            // 通过attr_value_agg获取属性值集合
            List<String> attrValue = ((ParsedStringTerms) bucket.getAggregations().get("attr_value_agg"))
                    .getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
            attrVo.setAttrId(attrId);
            attrVo.setAttrName(attrName);
            attrVo.setAttrValue(attrValue);
            attrs.add(attrVo);
        }
        result.setAttrs(attrs);

        // 4. 设置当前页
        result.setPageNum(param.getPageNum());

        // 5. 设置总记录数
        long total = hits.getTotalHits().value;
        result.setTotal(total);

        // 6. 设置总页数 - 计算得出
        long totalPage = total % EsConstant.PRODUCT_PAGESIZE == 0 ? total / EsConstant.PRODUCT_PAGESIZE : (total / EsConstant.PRODUCT_PAGESIZE + 1);
        result.setTotalPages((int) totalPage);

        // 7.  导航页
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPage; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);

        // 8. 面包屑导航
        if (!ObjectUtils.isEmpty(param.getAttrs())) {
            List<SearchResult.NavVo> navVos = param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] kv = attr.split("_");
                String k = kv[0];
                String v = kv[1];
                navVo.setNavValue(v);
                R r = productFeignService.attrInfo(Long.parseLong(k));
                result.getAttrIds().add(Long.parseLong(k));
                if (r.getCode() == 0) {
                    AttrResponseVo attrResponseVo = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    navVo.setNavName(attrResponseVo.getAttrName());
                }else {
                    navVo.setNavName(k);
                }

                String replace = replaceQuery(param,"attrs", attr);

                // 取消了这个面包屑以后我们要跳转到那个地方
                navVo.setLink("http://search.sinomall.com/list.html?"+replace);

                return navVo;
            }).collect(Collectors.toList());


            result.setNavs(navVos);
        }

        // 品牌、分类
        if (param.getBrandId() !=null && param.getBrandId().size()>0){
            List<SearchResult.NavVo> navVos = result.getNavs();
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavName("品牌");
            // TODO 远程查询所有品牌
            R r = productFeignService.brandInfos(param.getBrandId());
            if (r.getCode() == 0){
                List<BrandRespVo> brandRespVos = r.getData("brands", new TypeReference<List<BrandRespVo>>() {
                });
                StringBuffer sb = new StringBuffer();
                String replace ="";
                for (BrandRespVo brandRespVo : brandRespVos) {
                    sb.append(brandRespVo.getName() + ";");
                    replace = replaceQuery(param,"brandId", brandRespVo.getBrandId()+"");
                }
                navVo.setNavValue(sb.toString());
                navVo.setLink("http://search.sinomall.com/list.html?"+replace);
            }
            navVos.add(navVo);
            result.setNavs(navVos);
        }


        return result;
    }

    private String replaceQuery(SearchParam param,String key, String value) {
        String attrEndoce = null;
        try {
            attrEndoce = URLEncoder.encode(value, "UTF-8");
            if (attrEndoce.contains("+")){
                attrEndoce = attrEndoce.replace("+","%20"); //浏览器对空格编码和java不一样
            }

            if (attrEndoce.contains("%28")){
                attrEndoce = attrEndoce.replace("%28","("); //浏览器对空格编码和java不一样
            }

            if (attrEndoce.contains("%29")){
                attrEndoce = attrEndoce.replace("%29",")"); //浏览器对空格编码和java不一样
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replace = "";
        if (param.get_queryString().contains("&"+key+"="+ attrEndoce)){
            replace = param.get_queryString().replace("&"+key+"=" + attrEndoce, "");
        }else {
            replace = param.get_queryString().replace(key +"=" + attrEndoce, "");
        }
        return replace;
    }

    /**
     * 通过页面可能传递过来的查询条件，构建查询语句
     * #查询： 模糊匹配，过滤（按照属性，分类，品牌，价格区间，库存），排序，分页，高亮， 聚合分析？
     *
     * @param param
     * @return {@link SearchRequest}
     */

    private SearchRequest searchBuilder(SearchParam param) {
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        /**
         * 查询： ，过滤（按照属性，，）
         */

        // 1、 构建bool - query
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 1.1 must - 模糊匹配
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            boolQueryBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }

        // 1.2。1 bool - filter -按照三级分类过滤
        if (null != param.getCatalog3Id()) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }

        // 1.2.2 bool - filter -按照品牌id过滤
        if (!ObjectUtils.isEmpty(param.getBrandId())) {
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }

        // 1.2.3 bool - filter -按照属性过滤
        /**
         *  attrs=2_5寸：6寸
         */
        if (!ObjectUtils.isEmpty(param.getAttrs())) {
            for (String attrStr : param.getAttrs()) {
                BoolQueryBuilder attrQueryBuilder = QueryBuilders.boolQuery();
                String attrId = attrStr.split("_")[0];
                String values = attrStr.split("_")[1];

                attrQueryBuilder.must(QueryBuilders.termQuery("attrs.attrId", attrId)); //查询属性id
                attrQueryBuilder.must(QueryBuilders.termsQuery("attrs.attrValue", values.split(":"))); // 查询的属性值

                // 每一个都必须生成一个nested查询
                NestedQueryBuilder nestedQueryBuilder = QueryBuilders.nestedQuery("attrs", attrQueryBuilder, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder);
            }

        }

        // 1.2.3 bool - filter -按照价格区间过滤
        /**
         *   skuPrice=1_500/_500/500_
         */
        if (StringUtils.isNotEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("skuPrice");
            String[] skuPrice = StringUtils.trim(param.getSkuPrice()).split("_");
            if (skuPrice.length == 2) {
                rangeQueryBuilder.gt(skuPrice[0]).lt(skuPrice[1]);
            } else if (StringUtils.startsWith(param.getSkuPrice(), "_") && skuPrice.length > 0) {
                rangeQueryBuilder.lt(skuPrice[0]);
            } else if (StringUtils.endsWith(param.getSkuPrice(), "_") && skuPrice.length > 0) {
                rangeQueryBuilder.gt(skuPrice[0]);
            }

            boolQueryBuilder.filter(rangeQueryBuilder);
        }

        // 1.2.4 bool - filter -按照是否有库存过滤
        if (null != param.getHasStock())
            boolQueryBuilder.filter(QueryBuilders.termsQuery("hasStock", param.getHasStock() == 1));

        //把以前的所有条件都拿来进行封装
        sourceBuilder.query(boolQueryBuilder);

        /**
         * 排序，分页，高亮，
         */
        // 2.1 排序 sort=hotScore_asc/desc
        if (StringUtils.isNotEmpty(param.getSort())) {
            String[] s = param.getSort().split("_");
            String filed = s[0]; //需要排序的字段
            String sc = s[1]; // 升序或降序
            SortOrder sortOrder = sc.equals("asc") ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(filed, sortOrder);
        }

        // 2.2 分页 （pageNum -1）*pagesize
        sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
        sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);

        // 2.3 高亮
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }

        /**
         * 聚合分析
         */

        // TODO 1 对品牌的聚合
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        sourceBuilder.aggregation(brand_agg);

        // TODO 2 对分类的聚合
        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg").field("catalogId").size(50);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name").field("catalogName").size(1));
        sourceBuilder.aggregation(catalog_agg);

        // TODO 3 对属性的聚合
        // 嵌套聚合
        NestedAggregationBuilder nestedAggregationBuilder = AggregationBuilders.nested("attr_agg", "attrs");
        // 属性Id的聚合
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId").size(50);
        // 根据属性id对属性名字的聚合
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        // 根据属性id对属性值的聚合
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        // 嵌套聚合中设置属性id的聚合
        nestedAggregationBuilder.subAggregation(attr_id_agg);
        // 设置嵌套聚合
        sourceBuilder.aggregation(nestedAggregationBuilder);

        System.out.println("构建结果：" + sourceBuilder.toString());
        SearchRequest searchRequest = new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);

        return searchRequest;
    }
}
