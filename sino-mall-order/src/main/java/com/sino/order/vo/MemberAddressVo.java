package com.sino.order.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 会员收货地址
 * 
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 14:37:21
 */
@Data
public class MemberAddressVo implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * id
	 */
	private Long id;
	/**
	 * member_id
	 */
	private Long memberId;
	/**
	 * 收货人姓名
	 */
	private String name;
	/**
	 * 电话
	 */
	private String phone;
	/**
	 * 邮政编码
	 */
	private String postCode;
	/**
	 * 省份/直辖市
	 */
	private String province;
	/**
	 * 城市
	 */
	private String city;
	/**
	 * 区
	 */
	private String region;
	/**
	 * 详细地址(街道)
	 */
	private String detailAddress;
	/**
	 * 省市区代码
	 */
	private String areacode;
	/**
	 * 是否默认
	 */
	private Integer defaultStatus;

}
