package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * 权限树节点：组节点包含 children，操作点 checked 表示角色已勾选（A-L04）。
 */
public record PermissionNode(Long id, String name, String code, boolean checked,
                             List<PermissionNode> children) {
}
