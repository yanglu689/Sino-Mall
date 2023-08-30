package com.sino.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sino.common.utils.PageUtils;
import com.sino.ware.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author yanglu
 * @email 2318456591@qq.com
 * @date 2023-08-21 15:23:49
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

