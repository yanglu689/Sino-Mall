package com.sino.product.execption;


import com.sino.common.exception.BizCodeEnum;
import com.sino.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 集中处理异常
 *
 * @author yanglupc
 * @date 2023/08/27
 */
@Slf4j

//@ResponseBody
//@ControllerAdvice(basePackages = "com.sino.product.controlle")
@RestControllerAdvice(basePackages = "com.sino.product.controller")
public class SinoMallExceptionControllerAdvice {

    //    @ResponseBody
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleVaildException(MethodArgumentNotValidException e) {
        log.error("数据校验出现问题{},异常类型{}", e.getMessage(), e.getClass());
        BindingResult result = e.getBindingResult();
        Map<String, String> map = new HashMap();
        result.getFieldErrors().forEach((item) -> {
            String errMsg = item.getDefaultMessage();
            String field = item.getField();
            map.put(field, errMsg);
        });
        return R.error(BizCodeEnum.VAILD_EXECPTION.getCode(), BizCodeEnum.VAILD_EXECPTION.getMsg()).put("data", map);
    }

    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable) {
        log.error("errMsg:{}",throwable);
        return R.error(BizCodeEnum.UNKONG_EXECPTION.getCode(), BizCodeEnum.UNKONG_EXECPTION.getMsg());
    }

}
