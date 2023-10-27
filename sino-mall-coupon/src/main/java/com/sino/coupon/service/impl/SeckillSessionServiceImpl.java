package com.sino.coupon.service.impl;

import com.sino.coupon.entity.SeckillSkuRelationEntity;
import com.sino.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.coupon.dao.SeckillSessionDao;
import com.sino.coupon.entity.SeckillSessionEntity;
import com.sino.coupon.service.SeckillSessionService;
import org.springframework.util.ObjectUtils;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    private SeckillSkuRelationService relationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLatest3DaySession() {
        // 计算最近三天
        // Date date = new Date(); // 2023-10-12 23:52:00
        // LocalDate date = LocalDate.now(); // 年月日
        // LocalTime time ;// 时分秒
        // LocalDateTime localDateTime; // 时分秒 年月日
        List<SeckillSessionEntity> entityList = this.list(new QueryWrapper<SeckillSessionEntity>().between("start_time", start(), end()));
        // TODO 查出关联的商品、
        if (!ObjectUtils.isEmpty(entityList)){
            List<SeckillSessionEntity> seckillSessionEntities = entityList.stream().map(item -> {
                Long id = item.getId();
                // 按照活动id查询关系表
                List<SeckillSkuRelationEntity> relationEntities = relationService.list(new QueryWrapper<SeckillSkuRelationEntity>().eq("promotion_session_id", id));
                item.setRelationSkus(relationEntities);
                return item;
            }).collect(Collectors.toList());
            return seckillSessionEntities;
        }

        return null;
    }

    private String start(){
        LocalDate now = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(now, LocalTime.MIN);
        String format = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }

    private String end(){
        LocalDate now = LocalDate.now();
        LocalDate localDate = now.plusDays(2);
        LocalDateTime start = LocalDateTime.of(localDate, LocalTime.MAX);
        String format = start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return format;
    }

}