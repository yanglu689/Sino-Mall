package com.sino.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.sino.common.to.SkuEsModel;
import com.sino.search.config.SinoMallElasticSearchConfig;
import com.sino.search.constant.EsConstant;
import com.sino.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public Boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        // BulkRequest bulkRequest, RequestOptions options
        BulkRequest bulkRequest = new BulkRequest();
        skuEsModels.forEach(skuEsModel -> {
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(skuEsModel.getSkuId().toString());
            String source = JSON.toJSONString(skuEsModel);
            indexRequest.source(source, XContentType.JSON);
            bulkRequest.add(indexRequest);
        });

        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, SinoMallElasticSearchConfig.COMMON_OPTIONS);

        List<String> collect = Arrays.stream(bulk.getItems()).map(BulkItemResponse::getId).collect(Collectors.toList());
        log.info("商品上架成功：{}", collect);

        // 是否有错误
        return bulk.hasFailures();

    }
}
