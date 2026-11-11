package com.yunshu.zhixue.support.log;

import lombok.Getter;

/**
 * 操作日志类型（{@code operation_log.action} 存中文枚举值）。
 */
@Getter
public enum OperationAction {

    LOGIN("登录"),
    LOGOUT("登出"),
    CREATE("新增"),
    UPDATE("修改"),
    DELETE("删除"),
    STATUS_CHANGE("禁用启用"),
    EXPORT("导出"),
    OTHER("其他");

    private final String label;

    OperationAction(String label) {
        this.label = label;
    }
}
