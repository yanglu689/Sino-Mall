package com.sino.product.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class AttrRespVo extends AttrVo{

    /**
     * catelog名字
     */
    private String catelogName;

    /**
     * 分组名称
     */
    private String groupName;

    private Long attrGroupId;

    private Long[] catelogPath;
}
