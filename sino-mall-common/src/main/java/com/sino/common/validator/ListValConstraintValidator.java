package com.sino.common.validator;

import com.sino.common.validator.anno.ListVal;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

public class ListValConstraintValidator implements ConstraintValidator<ListVal, Integer> {

    private Set<Integer> set = new HashSet<Integer>();

    // 初始化方法
    @Override
    public void initialize(ListVal constraintAnnotation) {
        int[] vals = constraintAnnotation.vals();
        for (int i = 0; i < vals.length; i++) {
            set.add(vals[i]);
        }

    }

    //判断是否校验成功
    @Override
    public boolean isValid(Integer integer, ConstraintValidatorContext constraintValidatorContext) {

        return set.contains(integer);
    }
}
