package com.yunshu.zhixue.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.system.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色权限关联 Mapper。
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {
}
