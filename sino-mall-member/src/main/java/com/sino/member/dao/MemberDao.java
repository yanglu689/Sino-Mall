package com.sino.member.dao;

import com.sino.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 14:37:21
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
