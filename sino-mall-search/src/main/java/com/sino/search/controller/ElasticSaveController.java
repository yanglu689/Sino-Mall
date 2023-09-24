package com.sino.search.controller;

import com.sino.common.exception.BizCodeEnum;
import com.sino.common.to.SkuEsModel;
import com.sino.common.utils.R;
import com.sino.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("search/save")
public class ElasticSaveController {

    @Autowired
    private ProductSaveService productSaveService;

    @PostMapping("/product")
    public R productStatusUp(@RequestBody List<SkuEsModel> skuEsModels) {
        Boolean hasFailures = false;
        try {
            hasFailures = productSaveService.productStatusUp(skuEsModels);
        } catch (IOException e) {
            log.error("ElasticSaveController商品上架错误",e);
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }

        if (hasFailures) {
            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode(), BizCodeEnum.PRODUCT_UP_EXCEPTION.getMsg());
        }

        return R.ok();
    }
}
