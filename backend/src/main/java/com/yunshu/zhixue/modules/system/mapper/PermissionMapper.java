package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限表 Mapper。
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
