package com.sino.common.constant;

public class ProductConstant {

    public enum AttrTypeEnum{
        ATTR_SALE_TYPE(0, "销售属性"),ATTR_BASE_TYPE(1, "基本属性");

        AttrTypeEnum(int code, String msg){
            this.code = code;
            this.msg = msg;
        }

        private int code;

        private String msg;

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}
