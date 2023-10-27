package com.sino.order.Interceptor;

import com.sino.common.constant.AuthServerConstant;
import com.sino.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //  /order/order/status/{orderSn}
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean matchOrderStatusPath = antPathMatcher.match("/order/order/status/**", uri);  // spring提供的路径匹配
        boolean matchOrderTradePath = antPathMatcher.match("/trade/notify/**", uri);  // spring提供的路径匹配
        if (matchOrderStatusPath || matchOrderTradePath){
            return true;
        }

        MemberRespVo memberRespVo = (MemberRespVo) request.getSession().getAttribute(AuthServerConstant.LOGIN_USER);

        if (memberRespVo != null) {
            // 已登录
            loginUser.set(memberRespVo);
            return true;
        }else {
            // 未登录
            request.getSession().setAttribute("msg", "请先进行登录");
            response.sendRedirect("http://auth.sinomall.com/login.html");
            return false;
        }

    }
}
