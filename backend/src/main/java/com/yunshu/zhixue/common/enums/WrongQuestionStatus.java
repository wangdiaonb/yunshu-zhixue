package com.yunshu.zhixue.common.enums;

import lombok.Getter;

/**
 * 错题状态（{@code wrong_question.status}）：1 待复习 / 2 已掌握。
 */
@Getter
public enum WrongQuestionStatus {

    PENDING_REVIEW(1, "待复习"),
    MASTERED(2, "已掌握");

    private final int code;
    private final String label;

    WrongQuestionStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static WrongQuestionStatus fromCode(Integer code) {
        return code != null && code == MASTERED.code ? MASTERED : PENDING_REVIEW;
    }
}
