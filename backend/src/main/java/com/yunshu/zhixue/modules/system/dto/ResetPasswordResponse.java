package com.yunshu.zhixue.modules.system.dto;

/**
 * 重置密码响应：默认密码仅在本响应中明文返回一次（A-M07）。
 */
public record ResetPasswordResponse(String password) {
}
