package com.yunshu.zhixue.common.enums;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 题目类型（取值与 DB {@code question.type} 一致，API 直接传数值）。
 */
@Getter
public enum QuestionType {

    SINGLE_CHOICE(1, "单选"),
    MULTIPLE_CHOICE(2, "多选"),
    TRUE_FALSE(3, "判断"),
    SHORT_ANSWER(4, "简答");

    private final int code;
    private final String label;

    QuestionType(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static QuestionType fromCode(Integer code) {
        if (code != null) {
            for (QuestionType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
        }
        throw BizException.of(ErrorCode.PARAM_INVALID, "题目类型不合法");
    }

    /** 客观题（单选/多选/判断）本地判分，无需 AI。 */
    public boolean isObjective() {
        return this != SHORT_ANSWER;
    }
}
