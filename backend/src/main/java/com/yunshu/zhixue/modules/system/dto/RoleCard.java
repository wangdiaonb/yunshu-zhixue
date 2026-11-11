package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * 角色卡片（A-L01）。
 */
public record RoleCard(Long id, String name, String code, String description, long userCount,
                       List<RolePermissionTag> permissions, long totalPermissionCount,
                       boolean superAdmin) {
}
