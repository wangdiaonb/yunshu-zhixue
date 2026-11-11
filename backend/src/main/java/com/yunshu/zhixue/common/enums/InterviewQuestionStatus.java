package com.yunshu.zhixue.common.enums;

import lombok.Getter;

/**
 * 面试题状态：1 已上架 / 2 已下架。
 */
@Getter
public enum InterviewQuestionStatus {

    ONLINE(1, "已上架"),
    OFFLINE(2, "已下架");

    private final int code;
    private final String label;

    InterviewQuestionStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static InterviewQuestionStatus fromCode(Integer code) {
        return code != null && code == OFFLINE.code ? OFFLINE : ONLINE;
    }
}
