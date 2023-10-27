package com.sino.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.sino.common.exception.NoStockException;
import com.sino.common.to.mq.OrderTo;
import com.sino.common.to.mq.StockDetailTo;
import com.sino.common.to.mq.StockLockedTo;
import com.sino.common.utils.R;
import com.sino.ware.entity.WareOrderTaskDetailEntity;
import com.sino.ware.entity.WareOrderTaskEntity;
import com.sino.ware.feign.OrderFeignService;
import com.sino.ware.feign.ProductFeignService;
import com.sino.ware.service.WareOrderTaskDetailService;
import com.sino.ware.service.WareOrderTaskService;
import com.sino.ware.vo.OrderItemVo;
import com.sino.ware.vo.OrderVo;
import com.sino.ware.vo.SkuHasStockVo;
import com.sino.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.ware.dao.WareSkuDao;
import com.sino.ware.entity.WareSkuEntity;
import com.sino.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private WareOrderTaskService orderTaskService;

    @Autowired
    private WareOrderTaskDetailService orderTaskDetailService;

    @Autowired
    private OrderFeignService orderFeignService;

    @Override
    public void unLockStock(StockLockedTo lockedTo) {
        StockDetailTo detail = lockedTo.getDetail();
        Long detailId = detail.getId();
        // 解锁
        // 1。 查询数据库关于这个订单的锁定库存信息
        // 有：证明库存锁定成功
        //       解锁，需要根据订单情况
        //            1. 没有这个订单，必须解锁
        //            2. 有这个订单， 不是解锁库存
        //               先判断订单状态  已取消： 解锁库存  没取消：不能解锁
        // 没有：库存锁定失败，库存回滚，这种情况无需解锁
        WareOrderTaskDetailEntity byId = orderTaskDetailService.getById(detailId);
        if (byId != null){
            // 需要解锁
            Long id = lockedTo.getId();
            WareOrderTaskEntity taskEntity = orderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R orderStatus = orderFeignService.getOrderStatus(orderSn);
            if (orderStatus.getCode() == 0) {
                OrderVo orderVo = orderStatus.getData(new TypeReference<OrderVo>() {
                });
                if (orderVo== null || orderVo.getStatus() == 4) {
                    // 订单不存在
                    // 订单已经被取消，解锁库存
                    if (taskEntity.getTaskStatus() == 1){ //工作单状态为1：已锁定，才能解锁，否者不许
                        unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detail.getId());
                    }
                }
            }else {
                // 消息拒绝以后重新放入队列，让别人继续消费
                throw new RuntimeException("远程服务调用失败");
            }

        }else {
            // 无需解锁
        }

    }

    /**
     * un 锁定库存
     *
     * @param skuId        货号
     * @param wareId       软件编号
     * @param num          数字
     * @param taskDetailId 任务详细信息 ID
     */
    private void unLockStock(Long skuId, Long wareId, Integer num, Long taskDetailId){
        WareSkuDao baseMapper = this.getBaseMapper();
        baseMapper.unLockStock(skuId, wareId, num);
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setId(taskDetailId);
        taskEntity.setTaskStatus(2);  // 库存解锁成功，跟新工作单的状态
        orderTaskService.updateById(taskEntity);
    }


    /**
     * 查询页面
     * skuId: 123
     * wareId: 1
     *
     * @param params 参数个数
     * @return {@link PageUtils}
     */

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareSkuEntity> wrapper = new QueryWrapper<>();
        String skuId = (String) params.get("skuId");
        if (StringUtils.isNotEmpty(skuId) && !"0".equals(skuId)) {
            wrapper.eq("sku_id", skuId);
        }

        String wareId = (String) params.get("wareId");
        if (StringUtils.isNotEmpty(wareId) && !"0".equals(wareId)) {
            wrapper.eq("ware_id", wareId);
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
        if (ObjectUtils.isEmpty(wareSkuEntities)) {
            wareSkuEntity.setStockLocked(0);

            try {
                R r = productFeignService.skuInfo(wareSkuEntity.getSkuId());
                if (r.getCode() == 0) {
                    Map skuInfo = (Map) r.get("skuInfo");
                    wareSkuEntity.setSkuName((String) skuInfo.get("skuName"));
                }
            } catch (Exception e) {
                log.error("TODO 自己catch异常，不会使事务失效");

            }
            wareSkuDao.insert(wareSkuEntity);
        } else {
            wareSkuDao.updateStock(wareSkuEntity);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {
        List<SkuHasStockVo> skuHasStockVos = skuIds.stream().map(skuId -> {
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();
            Long count = baseMapper.getSkuStock(skuId);
            skuHasStockVo.setHasStock(count != null && count > 0L);
            skuHasStockVo.setSkuId(skuId);
            return skuHasStockVo;
        }).collect(Collectors.toList());
        return skuHasStockVos;
    }

    /**
     * 为某个订单锁定库存
     *
     * 库存解锁的场景
     * 1. 下订单成功， 订单过期没有支付被系统自动取消，被用户手动取消，都要解锁库存
     * 2. 下单成功， 库存锁定成功， 接下来的业务调用失败， 导致订单回滚， 之前的库存就要自动解锁
     *
     * @param vo 沃
     * @return {@link Boolean}
     */
    @Transactional
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        WareOrderTaskEntity wareOrderTaskEntity = new WareOrderTaskEntity();
        wareOrderTaskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskEntity.setTaskStatus(1);
        // 保存库存工作单
        orderTaskService.save(wareOrderTaskEntity);
        // 1. 按照下单的收获地址，找到一个就近的仓库，锁定库存

        // 1. 找到每一个商品在那个仓库都有库存
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> stocks = locks.stream().map(item -> {
            Long skuId = item.getSkuId();
            SkuWareHasStock skuWareHasStock = new SkuWareHasStock();
            skuWareHasStock.setSkuId(skuId);
            // 查询这个商品在哪里有库存
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
            skuWareHasStock.setWareId(wareIds);
            skuWareHasStock.setNum(item.getCount());
            return skuWareHasStock;
        }).collect(Collectors.toList());

        // 2. 锁定库存
        for (SkuWareHasStock stock : stocks) {
            Boolean skuLock = false;
            Long skuId = stock.getSkuId();
            List<Long> wareIds = stock.getWareId();
            if (ObjectUtils.isEmpty(wareIds)){
                // 在任何仓库中都没有库存
                throw new NoStockException("商品库存不足");
            }

            // 1. 如果每一个商品都锁定成功， 将当前商品锁定了几件的工作单记录发给mq
            // 2. 锁定失败， 前面保存的工作单信息就回滚了。发送出去的消息，即使要解锁记录，由于数据库查不到id,所以就不用解锁
            for (Long wareId : wareIds) {
                // 成功返回1， 否则是0
               Long size = wareSkuDao.lockSkuStock(skuId, wareId, stock.getNum());
                if (size == 1) {
                    // 成功锁库存成功
                    skuLock =true;
                    // TODO 告诉MQ库存锁定成功
                    WareOrderTaskDetailEntity detailEntity = new WareOrderTaskDetailEntity(null,skuId,"",stock.getNum(),wareOrderTaskEntity.getId(),wareId,1);
                    orderTaskDetailService.save(detailEntity);

                    StockLockedTo lockedTo = new StockLockedTo();
                    lockedTo.setId(wareOrderTaskEntity.getId());
                    StockDetailTo stockDetailTo = new StockDetailTo();
                    BeanUtils.copyProperties(detailEntity, stockDetailTo);
                    lockedTo.setDetail(stockDetailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked",lockedTo);
                    break;
                } else{
                    //   当前仓库锁库存失败，尝试下一个仓库
                }
            }

            if (!skuLock){
                throw new NoStockException("商品库存不足");
            }
        }

        // 3. 肯定全部都是锁定成功过的
        return true;
    }

    /**
     * 防止订单服务卡顿，导致订单状态消息一直改不了， 库存消息优先到期， 查订单状态为新建状态，什么都不做就走了
     * 导致卡顿的订单，永远不能解锁库存
     *
     * @param orderTo 订购到
     */
    @Transactional
    @Override
    public void unLockStock(OrderTo orderTo) {
        String orderSn = orderTo.getOrderSn();
        //根据订单号查询订单任务
        WareOrderTaskEntity taskEntity = orderTaskService.getOrderTaskByOrderSn(orderSn);
        //判断订单任务是否为空
        if (taskEntity != null){
            //获取订单任务id
            Long id = taskEntity.getId();
            //根据订单任务id和锁定状态查询订单任务详情
            List<WareOrderTaskDetailEntity> detailEntities = orderTaskDetailService.list(new QueryWrapper<WareOrderTaskDetailEntity>().eq("task_id", id).eq("lock_status", 1));
            //遍历订单任务详情
            detailEntities.forEach(item ->{
                //根据skuId,wareId,skuNum解锁库存
                unLockStock(item.getSkuId(), item.getWareId(), item.getSkuNum(), item.getId());
            });
        }
    }

    @Data
    class SkuWareHasStock{
        private Long skuId;
        private Integer num;
        private List<Long> wareId;

    }

}