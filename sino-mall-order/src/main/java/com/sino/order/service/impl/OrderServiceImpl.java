package com.sino.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.sino.common.exception.NoStockException;
import com.sino.common.to.mq.OrderTo;
import com.sino.common.to.mq.SeckillOrderTo;
import com.sino.common.utils.R;
import com.sino.common.vo.MemberRespVo;
import com.sino.order.Interceptor.LoginUserInterceptor;
import com.sino.order.constant.OrderConstant;
import com.sino.order.entity.OrderItemEntity;
import com.sino.order.entity.PaymentInfoEntity;
import com.sino.order.enume.OrderStatusEnum;
import com.sino.order.feign.CartFeignService;
import com.sino.order.feign.MemberFeignService;
import com.sino.order.feign.ProductFeignService;
import com.sino.order.feign.WareFeignService;
import com.sino.order.service.OrderItemService;
import com.sino.order.service.PaymentInfoService;
import com.sino.order.to.OrderCreateTo;
import com.sino.order.vo.*;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.order.dao.OrderDao;
import com.sino.order.entity.OrderEntity;
import com.sino.order.service.OrderService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> orderSubmitVoThreadLocal = new ThreadLocal<>();

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WareFeignService wareFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 确认订单
     * 返回订单确认页需要的数据
     *
     * @return {@link OrderConfirmVo}
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        OrderConfirmVo confirmVo = new OrderConfirmVo();

        // 通过spring提供的RequestContextHolder，拿到当前线程的request，异步编排线程不共享数据，需要重新设置请求
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            // 查询当前用户的收获地址
            // 异步不共享线程，需要重新设置请求后，配置的feignConfig中的request才不会丢失请求
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> memberAddress = memberFeignService.getMemberAddress(memberRespVo.getId());
            confirmVo.setAddress(memberAddress);
        }, executor);


        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            // 远程查询购物车选中的商品
            // feign远程调用会丢失请求头，需要重新将请求头中的cookie设置回去
            // 异步不共享线程，需要重新设置请求后，配置的feignConfig中的request才不会丢失请求
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(currentUserCartItems);
        }, executor).thenRunAsync(() -> {
            // 查询商品是否有货
            List<Long> skuIds = confirmVo.getItems().stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R skuHasStock = wareFeignService.getSkuHasStock(skuIds);
            if (skuHasStock.getCode() == 0) {
                List<SkuHasStockVo> hasStockVos = skuHasStock.getData(new TypeReference<List<SkuHasStockVo>>() {
                });
                if (hasStockVos != null) {
                    Map<Long, Boolean> collect = hasStockVos.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
                    confirmVo.setStocks(collect);
                }
            }
        }, executor);

        // 查询用户积分
        confirmVo.setIntegration(memberRespVo.getIntegration());

        // 其他数据自动计算

        // TODO 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");

        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, TimeUnit.MINUTES);

        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(addressFuture, cartFuture).get();

        return confirmVo;
    }

    // 同一个对象内事务方法互调默认失效  绕过了代理对象

    // 事务使用代理对象来控制的
    @Transactional(timeout = 30)
    public void a(){
        // b,c做任何设置都没用。都是和a公用一个事务，因为没有使用代理的对象调用b,c方法
        // b();
        // c();

        // 使用aspectj提供的动态代理获取当前类的代理对象
        OrderServiceImpl orderService =(OrderServiceImpl) AopContext.currentProxy();
        orderService.b();
        orderService.c();
        int i = 10/0;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void b(){

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void c(){

    }

    /**
     * 提交订单
     *  1. 本地事务，在分布式系，只能控制住自己的回滚，控制不住其他服务的回滚
     *  2. 分布式事务最大原因是 网络问题+分布式机器
     *
     * @param vo 沃
     * @return {@link SubmitOrderResponseVo}
     */
    //isolation = Isolation.READ_COMMITTED 设置隔离级别
    //propagation = Propagation.REQUIRES_NEW 设置传播行为
    //timeout = 30 设置超时时间
    // @GlobalTransactional //不适合高并发场景
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        orderSubmitVoThreadLocal.set(vo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();

        // 1. 验证令牌 【令牌的验证和对比必须保证原子性】
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        String orderToken = vo.getOrderToken();

        String redisTokenkey = OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId();
        // 原子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(redisTokenkey), orderToken);

        if (result == 0L) {
            // 验证失败
            responseVo.setCode(1);
            return responseVo;
        } else {
            // 验证成功
            // 去创建订单，验证令牌，验证价格，锁库存。。。
            // 1. 创建订单，订单项信息
            OrderCreateTo order = createOrder();
            // 2. 验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 金额对比
                // TODO 3. 保存订单
                saveOrder(order);

                // 4. 库存锁定，只要有异常回滚订单数据，
                // 订单号，所有订单项（skuid, skuName, num）
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> collect = order.getOrderitems().stream().map(orderItem -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(orderItem.getSkuId());
                    itemVo.setCount(orderItem.getSkuQuantity());
                    itemVo.setTitle(orderItem.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());

                lockVo.setLocks(collect);
                // TODO 4. 远程锁库存
                // 库存锁定成功，但是由于网络/超时原因，导致订单回滚，库存不回滚
                R r = wareFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    // 锁定成功
                    responseVo.setOrder(order.getOrder());

                    // TODO 5. 远程扣减积分
                    // int i = 10/0;   //出现问题，，订单回滚，库存不回滚
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    return responseVo;
                }else {
                    // 锁定失败
                    responseVo.setCode(3);
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);

                }
                
            }else {
                responseVo.setCode(2); // 金额对比失败
                return responseVo;
            }
        }
        // String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        // if (orderToken != null && orderToken.equals(redisToken)) {
        //     // 令牌验证通过
        //     redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()); // 删除令牌
        // }else {
        //     // 令牌验证失败
        // }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity orderEntity = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return orderEntity;
    }

    @Override
    public void closeOrder(OrderEntity orderEntity) {
        OrderEntity byId = this.getById(orderEntity.getId());
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity entity = new OrderEntity();
            entity.setId(orderEntity.getId());
            entity.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(entity);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(byId, orderTo);
            try {
                // TODO 保证消息一定会发出去， 每一个消息都可以做好日志记录（给数据库保存每一个消息的详细信息）
                // TODO 定期扫描数据库， 将失败的消息在发送一遍
                rabbitTemplate.convertAndSend("order-event-exchange","order.release.other",orderTo);
            } catch (AmqpException e) {
                // TODO 将没发送成功的的消息进行重试发送
            }
        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        // 根据orderSn查询订单，并设置PayVo的各个属性
        OrderEntity orderEntity = this.getOrderByOrderSn(orderSn);
        payVo.setOut_trade_no(orderSn);
        // 处理付款金额 取小数后两位，并向上取值
        BigDecimal payAmount = orderEntity.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(payAmount.toString());

       //根据订单号查询订单商品信息
        List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity orderItemEntity = orderItemEntities.get(0);
        payVo.setSubject(orderItemEntity.getSkuName());
        payVo.setBody(orderItemEntity.getSkuAttrsVals());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
       //获取当前登录用户信息
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberRespVo.getId()).orderByDesc("id")
        );

        List<OrderEntity> orderEntities = page.getRecords();

        List<OrderEntity> collect = orderEntities.stream().map(record -> {
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", record.getOrderSn()));
            record.setItems(orderItemEntities);
            return record;
        }).collect(Collectors.toList());

        page.setRecords(collect);
        return new PageUtils(page);
    }

    /**
     * 处理支付结果
     *
     * @param vo
     * @return {@link String}
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        // 根据异步支付vo封装支付实体类
        PaymentInfoEntity payInfo = new PaymentInfoEntity();
        payInfo.setAlipayTradeNo(vo.getTrade_no());
        payInfo.setOrderSn(vo.getOut_trade_no());
        payInfo.setPaymentStatus(vo.getTrade_status());
        payInfo.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(payInfo);

        // 修改订单的状态信息
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            String orderSn = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode());
        }

        return "success";
    }

    /**
     * 创建秒杀订单
     *
     * @param seckillOrderTo 秒杀信息
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrderTo) {
        // TODO 保存订单信息
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrderTo.getOrderSn());
        orderEntity.setMemberId(seckillOrderTo.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());

        BigDecimal payAmount = seckillOrderTo.getSeckillPrice().multiply(new BigDecimal("" + seckillOrderTo.getNum()));
        orderEntity.setPayAmount(payAmount);
        this.save(orderEntity);

        // TODO 保存订单项信息,
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setOrderSn(seckillOrderTo.getOrderSn());
        itemEntity.setRealAmount(payAmount);
        itemEntity.setSkuQuantity(seckillOrderTo.getNum());

        // TODO 获取当前spu的相信信息进行设置
        // R r = productFeignService.getSpuInfoBySkuId(seckillOrderTo.getSkuId());
        orderItemService.save(itemEntity);
    }

    /**
     * 保存订单
     * 保存订单数据
     *
     * @param order 订单
     */
    private void saveOrder(OrderCreateTo order) {
        // 保存订单
        OrderEntity orderEntity = order.getOrder();
        this.save(orderEntity);

        //保存订单项
        List<OrderItemEntity> orderitems = order.getOrderitems();
        orderItemService.saveBatch(orderitems);
    }


    /**
     * 创建订单
     *
     * @return {@link OrderCreateTo}
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();

        // 订单号使用mybatis-plus提供的工具
        String orderSn = IdWorker.getTimeId();

        // 1. 生成订单号
        OrderEntity orderEntity = buildOrder(orderSn);
        // 2. 获取到所有的订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        // 3. 验价
        computePrice(orderEntity, orderItemEntities);
        // 赋值
        createTo.setOrder(orderEntity);
        createTo.setOrderitems(orderItemEntities);

        return createTo;

    }


    /**
     * 计算价格
     *
     * @param orderEntity       订单实体
     * @param orderItemEntities 订单项实体
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal totalPrice = new BigDecimal("0");

        BigDecimal coupon = new BigDecimal("0");
        BigDecimal integration = new BigDecimal("0");
        BigDecimal promotion = new BigDecimal("0");

        Integer gift = 0; //积分
        Integer growth = 0; //成长值

        // 订单总额，叠加每一个订单项的总额信息
        for (OrderItemEntity itemEntity : orderItemEntities) {
            totalPrice = totalPrice.add(itemEntity.getRealAmount());
            coupon = coupon.add(itemEntity.getCouponAmount());
            integration = integration.add(itemEntity.getIntegrationAmount());
            promotion = promotion.add(itemEntity.getPromotionAmount());
            gift += itemEntity.getGiftIntegration();
            growth += itemEntity.getGiftGrowth();
        }

        // 1. 订单价格相关
        orderEntity.setTotalAmount(totalPrice); // 订单项总额
        orderEntity.setPayAmount(totalPrice.add(orderEntity.getFreightAmount())); // 支付总额 = 订单总额 - 运费
        orderEntity.setCouponAmount(coupon);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setPromotionAmount(promotion);

        // 2. 设置成长值
        orderEntity.setIntegration(gift);
        orderEntity.setGrowth(growth);
        orderEntity.setDeleteStatus(0);// 未删除
    }

    /**
     * 构建订单项
     *
     * @return {@link List}<{@link OrderItemEntity}>
     * @param orderSn
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        // 2. 获取到所有的订单项
        // 最后确认每个购物项的价格
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
        if (ObjectUtils.isNotEmpty(currentUserCartItems)) {
            List<OrderItemEntity> itemEntitys = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntitys;
        }
        return null;
    }

    /**
     * 构建每一个订单项
     *
     * @param cartItem 购物车项目
     * @return {@link OrderItemEntity}
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItem = new OrderItemEntity();
        // 1. 设置订单号  √
        // 2. 商品的spu信息
        Long skuId = cartItem.getSkuId();
        R rSpu = productFeignService.getSpuInfoBySkuId(skuId);
        if (rSpu.getCode() == 0) {
            SpuInfoVo spuInfo = rSpu.getData(new TypeReference<SpuInfoVo>() {
            });
            orderItem.setSpuId(spuInfo.getId());
            orderItem.setSpuBrand(spuInfo.getBrandId().toString());
            orderItem.setSpuName(spuInfo.getSpuName());
            orderItem.setCategoryId(spuInfo.getCatalogId());
        }
        // 3. 商品的sku信息 √
        orderItem.setSkuId(cartItem.getSkuId());
        orderItem.setSkuName(cartItem.getTitle());
        orderItem.setSkuPic(cartItem.getImage());
        orderItem.setSkuPrice(cartItem.getPrice());
        String saleAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        orderItem.setSkuAttrsVals(saleAttr);
        orderItem.setSkuQuantity(cartItem.getCount());
        // 4. 优惠信息【不做】
        // 5. 积分信息 √
        orderItem.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue()/10);
        orderItem.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue()/10);

        // 6. 订单价格信息
        orderItem.setPromotionAmount(new BigDecimal("0"));
        orderItem.setCouponAmount(new BigDecimal("0"));
        orderItem.setIntegrationAmount(new BigDecimal("0"));
        // 当前订单项的实际金额， 总额-各种优惠
        BigDecimal orign = orderItem.getSkuPrice().multiply(new BigDecimal(orderItem.getSkuQuantity().toString()));
        BigDecimal realAmount = orign.subtract(orderItem.getCouponAmount()).subtract(orderItem.getPromotionAmount()).subtract(orderItem.getIntegrationAmount());
        orderItem.setRealAmount(realAmount);

        return orderItem;
    }

    /**
     * 构建订单
     *
     * @return {@link OrderEntity}
     */
    private OrderEntity buildOrder(String orderSn) {
        OrderEntity orderEntity = new OrderEntity();
        // 1. 生成订单号 使用mybatis-plus提供的工具
        orderEntity.setOrderSn(orderSn);
        // 获取收货地址信息
        OrderSubmitVo orderSubmitVo = orderSubmitVoThreadLocal.get();
        R rFare = wareFeignService.getFare(orderSubmitVo.getAddrId());
        if (rFare.getCode() == 0) {
            FareVo fareVo = rFare.getData(new TypeReference<FareVo>() {
            });

            // 运费金额
            orderEntity.setFreightAmount(fareVo.getFare());
            // 设置收货人信息
            MemberAddressVo address = fareVo.getAddress();
            orderEntity.setMemberId(address.getMemberId());
            orderEntity.setReceiverCity(address.getCity());
            orderEntity.setReceiverDetailAddress(address.getDetailAddress());
            orderEntity.setReceiverName(address.getName());
            orderEntity.setReceiverPhone(address.getPhone());
            orderEntity.setReceiverPostCode(address.getPostCode());
            orderEntity.setReceiverProvince(address.getProvince());
            orderEntity.setReceiverRegion(address.getRegion());

            // 设置订单的相关状态信息
            orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
            orderEntity.setAutoConfirmDay(7);
        }
        return orderEntity;
    }

}