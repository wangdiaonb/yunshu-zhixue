package com.yunshu.zhixue.common.enums;

import lombok.Getter;

/**
 * 账号状态（学员 {@code student.status} 与后台账号 {@code admin_user.status} 共用：1 启用 / 0 禁用）。
 */
@Getter
public enum AccountStatus {

    DISABLED(0, "已禁用"),
    ENABLED(1, "已启用");

    private final int code;
    private final String label;

    AccountStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static AccountStatus fromCode(Integer code) {
        return code != null && code == ENABLED.code ? ENABLED : DISABLED;
    }

    public boolean isEnabled() {
        return this == ENABLED;
    }
}
