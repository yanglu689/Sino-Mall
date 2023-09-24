package com.sino.search;

import com.alibaba.fastjson.JSON;
import com.sino.search.config.SinoMallElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.aggregations.metrics.AvgAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

@SpringBootTest
class SinoMallSearchApplicationTests {

    @Autowired
    private RestHighLevelClient client;

    @ToString
    @Data
    static class Account {

        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }

    @Test
    public void searchData() throws IOException {
        // 创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        //指定索引
        searchRequest.indices("bank");
        //指定DSL 检索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建检索条件
        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));
        TermsAggregationBuilder ageAggr = AggregationBuilders.terms("ageAggr").field("age").size(10);
        searchSourceBuilder.aggregation(ageAggr);

        AvgAggregationBuilder balanceAggr = AggregationBuilders.avg("balanceAggr").field("balance");
        searchSourceBuilder.aggregation(balanceAggr);

        System.out.println("检索条件" + searchSourceBuilder.toString());
        searchRequest.source(searchSourceBuilder);

        // 检索
        SearchResponse searchResponse = client.search(searchRequest, SinoMallElasticSearchConfig.COMMON_OPTIONS);


        //分析结果
        System.out.println(searchResponse.toString());

        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();

        // 获取命中记录
        for (SearchHit hit : searchHits) {
//            hit.getIndex();
//            hit.getType();
//            hit.getId();

            String source = hit.getSourceAsString();
            Account account = JSON.parseObject(source, Account.class);
            System.out.println("accout" + account.toString());
        }

        // 获取聚合信息
        Aggregations aggregations = searchResponse.getAggregations();
//        List<Aggregation> aggregations1 = aggregations.asList();
//        for (Aggregation aggregation : aggregations1) {
//            System.out.println("当前聚合："+aggregation.getName());
//        }

        Terms ageAggr1 = aggregations.get("ageAggr");
        for (Terms.Bucket bucket : ageAggr1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄：" + keyAsString + "有几个人：" + bucket.getDocCount());
        }

        Avg balanceAggr1 = aggregations.get("balanceAggr");
        System.out.println("平均薪资："+ balanceAggr1.getValueAsString());

    }


    @Test
    void contextLoads() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
//        indexRequest.source("userName","zhangsan","age","男");

        User user = new User();
        user.setAge(1);
        user.setName("zhangsan ");
        user.setGender("nan");
        indexRequest.source(JSON.toJSONString(user), XContentType.JSON);
        // 执行
        client.index(indexRequest, SinoMallElasticSearchConfig.COMMON_OPTIONS);

        // 操作
        System.out.println(indexRequest);

    }

    @Data
    class User {
        private String name;
        private String gender;
        private int age;
    }

}
