package com.sino.ware.service.impl;

import com.sino.common.constant.WareConstant;
import com.sino.ware.entity.PurchaseDetailEntity;
import com.sino.ware.entity.WareSkuEntity;
import com.sino.ware.service.PurchaseDetailService;
import com.sino.ware.service.WareSkuService;
import com.sino.ware.vo.MergeVo;
import com.sino.ware.vo.PurchaseDoneVo;
import com.sino.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.ware.dao.PurchaseDao;
import com.sino.ware.entity.PurchaseEntity;
import com.sino.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService purchaseDetailService;

    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryUnreceiveListPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", 0).or().eq("status", 1)
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        // 如果采购单的id为空则需要新建采购单，然后合并
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATEID.getCode());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }

        // 需要合并的采购项
        List<Long> purchaseDetailIds = mergeVo.getItems();
        if (!ObjectUtils.isEmpty(purchaseDetailIds)) {
            Long finalPurchaseId = purchaseId;
            List<PurchaseDetailEntity> purchaseDetailEntities = purchaseDetailIds.stream().filter(id -> {
                // 判断采购项是否正在采购中
                PurchaseDetailEntity byId = purchaseDetailService.getById(id);
                return byId != null && byId.getStatus() != WareConstant.PurchaseDetailStatusEnum.BUYING.getCode();
            }).map(id -> {
                PurchaseDetailEntity purchaseDetailEntity = new PurchaseDetailEntity();
                purchaseDetailEntity.setId(id);
                purchaseDetailEntity.setPurchaseId(finalPurchaseId);
                purchaseDetailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
                return purchaseDetailEntity;
            }).collect(Collectors.toList());

            purchaseDetailService.updateBatchById(purchaseDetailEntities);
        }

        PurchaseEntity entity = new PurchaseEntity();
        entity.setId(purchaseId);
        entity.setUpdateTime(new Date());
        this.updateById(entity);


    }

    @Override
    public void receivedPurchase(List<Long> purchaseIds) {
        if (ObjectUtils.isEmpty(purchaseIds)) {
            return;
        }

        //查找采购订单并修改采购订单状态
        List<PurchaseEntity> purchaseEntities = purchaseIds.stream().map(id -> {
            PurchaseEntity purchaseEntity = this.getById(id);
            return purchaseEntity;
        }).filter(item -> {
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATEID.getCode() || item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            } else {
                return false;
            }
        }).map(item -> {
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());

        this.updateBatchById(purchaseEntities);

        // 修改采购项（detail表）的状态
        purchaseEntities.forEach(item -> {
            List<PurchaseDetailEntity> entities = purchaseDetailService.updateStatusByPurchaseId(item.getId());
            if (!ObjectUtils.isEmpty(entities)) {
                // 查出采购订单对应的采购项集合
                List<PurchaseDetailEntity> collect = entities.stream().map(e -> {
                    PurchaseDetailEntity entity = new PurchaseDetailEntity();
                    entity.setId(e.getId());
                    entity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                    return entity;
                }).collect(Collectors.toList());

                // 跟新状态
                purchaseDetailService.updateBatchById(collect);
            }
        });

    }

    @Transactional
    @Override
    public void purchaseDone(PurchaseDoneVo purchaseDoneVo) {
        if (purchaseDoneVo.getId() == null){
            return;
        }
        // 更新采购项的状态
        List<PurchaseItemDoneVo> items = purchaseDoneVo.getItems();

        // 需要更新的采购项集合
        List<PurchaseDetailEntity> updateList = new ArrayList<>();
        // 采购项状态标识位
        boolean statusFlag = true;
        if (items != null) {
            for (PurchaseItemDoneVo item : items) {
                PurchaseDetailEntity entity = new PurchaseDetailEntity();
                if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                    statusFlag = false;
                    entity.setStatus(item.getStatus());
                }else {
                    entity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                    PurchaseDetailEntity detailEntity = purchaseDetailService.getById(item.getItemId());
                    if (!ObjectUtils.isEmpty(detailEntity)){
                        WareSkuEntity wareSkuEntity = new WareSkuEntity();
                        wareSkuEntity.setSkuId(detailEntity.getSkuId());
                        wareSkuEntity.setWareId(detailEntity.getWareId());
                        wareSkuEntity.setStock(detailEntity.getSkuNum());
                        // 采购入库
                        wareSkuService.addOrUpdateStock(wareSkuEntity);
                    }

                }
                entity.setId(item.getItemId());
                updateList.add(entity);
            }
            // 批量更新采购项的状态
            purchaseDetailService.updateBatchById(updateList);
        }

        // 调整采购单的状态、
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseDoneVo.getId());
        if (!statusFlag){
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        }else {
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.FINISH.getCode());
        }
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

}