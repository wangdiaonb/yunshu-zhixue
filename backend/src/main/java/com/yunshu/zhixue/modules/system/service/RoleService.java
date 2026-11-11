package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.PermissionNode;
import com.yunshu.zhixue.modules.system.dto.RoleCard;
import com.yunshu.zhixue.modules.system.dto.RoleCreateRequest;
import com.yunshu.zhixue.modules.system.dto.RoleOption;

import java.util.List;

/**
 * 角色与权限服务（仅超级管理员可操作）。
 */
public interface RoleService {

    List<RoleCard> listRoles();

    Long createRole(RoleCreateRequest request);

    void deleteRole(Long roleId);

    /** 权限树；roleId 为空时全部未勾选。 */
    List<PermissionNode> permissionTree(Long roleId);

    /** 保存角色权限：全量覆盖。 */
    void savePermissions(Long roleId, List<Long> permissionIds);

    /** 用户管理使用的角色下拉选项。 */
    List<RoleOption> listOptions();
}
