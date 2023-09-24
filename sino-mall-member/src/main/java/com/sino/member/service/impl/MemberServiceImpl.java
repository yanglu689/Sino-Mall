package com.sino.member.service.impl;

import com.sino.member.dao.MemberLevelDao;
import com.sino.member.entity.MemberLevelEntity;
import com.sino.member.exception.PhoneExistException;
import com.sino.member.exception.UserNameExistException;
import com.sino.member.service.MemberLevelService;
import com.sino.member.vo.MemberLoginVo;
import com.sino.member.vo.MemberRegistVo;
import com.sino.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.member.dao.MemberDao;
import com.sino.member.entity.MemberEntity;
import com.sino.member.service.MemberService;
import org.springframework.util.ObjectUtils;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    private MemberLevelDao levelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo registVo) {
        MemberDao memberDao = this.getBaseMapper();

        MemberEntity entity = new MemberEntity();

        // 查询默认会员等级
        MemberLevelEntity levelEntity = levelDao.getDefaultLevel();

        entity.setLevelId(levelEntity.getId());

        // 检查用户名和手机号是否唯一，如果不唯一直接抛出异常让上游感知
        userNameUnique(registVo.getUserName());

        phoneUnique(registVo.getPhone());

        entity.setUsername(registVo.getUserName());
        entity.setNickname(registVo.getUserName());
        entity.setMobile(registVo.getPhone());

        // 使用spring中的bcrupt加密密码
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        String encodePassword = bCryptPasswordEncoder.encode(registVo.getPassword());
        entity.setPassword(encodePassword);
        memberDao.insert(entity);
    }

    @Override
    public void userNameUnique(String userName) throws UserNameExistException {
        int count = this.count(new QueryWrapper<MemberEntity>().eq("username", userName));
        if (count > 0) {
            throw new UserNameExistException();
        }
    }

    @Override
    public void phoneUnique(String phone) throws PhoneExistException {
        int count = this.count(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo memberLoginVo) {
        MemberDao baseMapper = this.baseMapper;
        String loginacct = memberLoginVo.getLoginacct();
        String password = memberLoginVo.getPassword();

        MemberEntity entity = baseMapper.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));

        if (null == entity){
            return null;
        }
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        boolean matches = bCryptPasswordEncoder.matches(password, entity.getPassword());
        if (matches){
            return entity;
        }
        return null;
    }

    @Override
    public MemberEntity login(SocialUser socialUser) {
        MemberDao memberDao = this.baseMapper;
        String uid = socialUser.getUid();

        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (ObjectUtils.isEmpty(memberEntity)){
            // 无此用户，需要进行注册
            MemberEntity regist = new MemberEntity();
            regist.setEmail(socialUser.getEmail());
            regist.setNickname(socialUser.getName());

            regist.setSocialUid(socialUser.getUid());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(socialUser.getExpires_in());
            memberDao.insert(regist);
            return regist;
        }else {
            // 有此社交用户，无需注册，直接更新用户信息后，返回用户信息
            MemberEntity updateEntity = new MemberEntity();
            updateEntity.setId(memberEntity.getId());
            // 更新
            updateEntity.setAccessToken(socialUser.getAccess_token());
            updateEntity.setExpiresIn(socialUser.getExpires_in());
            memberDao.updateById(updateEntity);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(socialUser.getExpires_in());
            return memberEntity;
        }
    }
}