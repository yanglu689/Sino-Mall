package com.sino.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sino.common.utils.PageUtils;
import com.sino.member.entity.MemberEntity;
import com.sino.member.exception.PhoneExistException;
import com.sino.member.exception.UserNameExistException;
import com.sino.member.vo.MemberLoginVo;
import com.sino.member.vo.MemberRegistVo;
import com.sino.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 14:37:21
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo registVo);

    void userNameUnique(String userName) throws UserNameExistException;

    void phoneUnique(String phone) throws PhoneExistException;

    MemberEntity login(MemberLoginVo memberLoginVo);

    MemberEntity login(SocialUser socialUser);
}

