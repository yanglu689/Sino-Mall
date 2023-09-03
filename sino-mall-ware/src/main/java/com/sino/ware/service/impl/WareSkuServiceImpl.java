package com.sino.ware.service.impl;

import com.sino.common.utils.R;
import com.sino.ware.entity.WareInfoEntity;
import com.sino.ware.feign.ProductFeignService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.ware.dao.WareSkuDao;
import com.sino.ware.entity.WareSkuEntity;
import com.sino.ware.service.WareSkuService;
import org.springframework.util.ObjectUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private ProductFeignService productFeignService;

    /**
     * 查询页面
     * skuId: 123
     * wareId: 1
     * @param params 参数个数
     * @return {@link PageUtils}
     */

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (StringUtils.isNotEmpty(skuId) && !"0".equals(skuId)) {
            wrapper.eq("sku_id",skuId);
        }

        String wareId = (String) params.get("wareId");
        if (StringUtils.isNotEmpty(wareId) && !"0".equals(wareId)) {
            wrapper.eq("ware_id",wareId);
        }

        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addOrUpdateStock(WareSkuEntity wareSkuEntity) {
        List<WareSkuEntity> wareSkuEntities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", wareSkuEntity.getSkuId()).eq("ware_id", wareSkuEntity.getWareId()));
        if (ObjectUtils.isEmpty(wareSkuEntities)){
            wareSkuEntity.setStockLocked(0);

            try {
                R r = productFeignService.skuInfo(wareSkuEntity.getSkuId());
                if (r.getCode() == 0) {
                    Map skuInfo = (Map) r.get("skuInfo");
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            }catch (Exception e){
                log.error("TODO 自己catch异常，不会使事务失效");

            }
            wareSkuDao.insert(wareSkuEntity);
        }else {
            wareSkuDao.updateStock(wareSkuEntity);
        }
    }

}