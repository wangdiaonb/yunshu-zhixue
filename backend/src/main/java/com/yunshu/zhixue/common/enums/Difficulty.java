package com.yunshu.zhixue.common.enums;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 难度（题目 / 面试题共用，取值 1 简单 / 2 中等 / 3 困难）。
 */
@Getter
public enum Difficulty {

    EASY(1, "简单"),
    MEDIUM(2, "中等"),
    HARD(3, "困难");

    private final int code;
    private final String label;

    Difficulty(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Difficulty fromCode(Integer code) {
        if (code != null) {
            for (Difficulty difficulty : values()) {
                if (difficulty.code == code) {
                    return difficulty;
                }
            }
        }
        throw BizException.of(ErrorCode.PARAM_INVALID, "难度不合法");
    }
}
