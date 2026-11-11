package com.yunshu.zhixue.common.enums;

import lombok.Getter;

/**
 * 性别（{@code student.gender}）：0 未知 / 1 男 / 2 女。
 */
@Getter
public enum Gender {

    UNKNOWN(0, "未知"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final int code;
    private final String label;

    Gender(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Gender fromCode(Integer code) {
        if (code != null) {
            for (Gender gender : values()) {
                if (gender.code == code) {
                    return gender;
                }
            }
        }
        return UNKNOWN;
    }
}
