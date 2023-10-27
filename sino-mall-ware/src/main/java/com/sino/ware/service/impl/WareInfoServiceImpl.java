package com.sino.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.sino.common.utils.R;
import com.sino.ware.feign.MemberFeignService;
import com.sino.ware.vo.FareVo;
import com.sino.ware.vo.MemberAddressVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.ware.dao.WareInfoDao;
import com.sino.ware.entity.WareInfoEntity;
import com.sino.ware.service.WareInfoService;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (StringUtils.isNotEmpty(key)){
            wrapper.and(w -> {
                w.eq("id", key).or().like("name",key).or().like("address",key).or().like("areacode",key);
            });
        }

        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据收获地址计算运费
     *
     * @param addrId 地址标识
     * @return {@link BigDecimal}
     */
    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();
        R info = memberFeignService.info(addrId);
        if (info.getCode()==0) {
            MemberAddressVo memberReceiveAddress = info.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {
            });

            fareVo.setAddress(memberReceiveAddress);
            // TODO 调用第三方接口查询运费信息
            String phone = memberReceiveAddress.getPhone();
            String substring = phone.substring(phone.length() - 1, phone.length());
            fareVo.setFare(new BigDecimal(substring));
            return fareVo;
        }
        return null;
    }

}