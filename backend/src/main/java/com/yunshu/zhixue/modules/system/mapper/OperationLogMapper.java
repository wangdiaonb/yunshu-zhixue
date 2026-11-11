package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper。
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
