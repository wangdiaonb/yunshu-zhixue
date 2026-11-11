package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;

/**
 * 后台用户列表项（时间已转换为 Asia/Shanghai）。
 */
public record AdminUserItem(Long id, String username, String mobile, String email,
                            Long roleId, String roleName, String roleCode,
                            Integer status, Integer isBuiltin,
                            LocalDateTime lastLoginAt, LocalDateTime createdAt) {
}
