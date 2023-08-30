package com.sino.order.dao;

import com.sino.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 15:20:12
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
