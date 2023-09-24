package com.sino.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.sino.common.exception.BizCodeEnum;
import com.sino.member.exception.PhoneExistException;
import com.sino.member.exception.UserNameExistException;
import com.sino.member.feign.CouponFeignService;
import com.sino.member.vo.MemberLoginVo;
import com.sino.member.vo.MemberRegistVo;
import com.sino.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.sino.member.entity.MemberEntity;
import com.sino.member.service.MemberService;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.R;



/**
 * 会员
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 14:37:21
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeignService couponFeignService;

    @RequestMapping("/test")
    public R testCoupon(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("zhangshan");
        R r = couponFeignService.memberCoupon();
        Object coupon = r.get("coupon");

        return R.ok().put("member",memberEntity).put("coupon",coupon);
    }
    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo registVo){

        try {
            memberService.regist(registVo);
        } catch (UserNameExistException e) {
            return R.error(BizCodeEnum.USER_EXIST_EXCEPTION.getCode(), BizCodeEnum.USER_EXIST_EXCEPTION.getMsg());
        } catch (PhoneExistException e) {
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo memberLoginVo){
        MemberEntity entity = memberService.login(memberLoginVo);
        if (null == entity){
           return R.error(BizCodeEnum.USERACCT_PASSWORD_INVAILD_EXCEPTION.getCode(), BizCodeEnum.USERACCT_PASSWORD_INVAILD_EXCEPTION.getMsg());
        }

        // 登录成功
        return R.ok().setData(entity);
    }

    @PostMapping("/oauth2/login")
    public R oauthLogin(@RequestBody SocialUser socialUser){
        MemberEntity entity = memberService.login(socialUser);
        if (null == entity){
            return R.error(BizCodeEnum.USERACCT_PASSWORD_INVAILD_EXCEPTION.getCode(), BizCodeEnum.USERACCT_PASSWORD_INVAILD_EXCEPTION.getMsg());
        }

        return R.ok().setData(entity);
    }

}
