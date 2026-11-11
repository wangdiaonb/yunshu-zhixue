package com.yunshu.zhixue.common.enums;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 题目状态。
 *
 * <p>{@link #DRAFT} 为数据库预留值，接口不接受该入参、筛选下拉不展示（Q6 定稿）。</p>
 */
@Getter
public enum QuestionStatus {

    ONLINE(1, "已上架"),
    OFFLINE(2, "已下架"),
    PENDING(3, "待审核"),
    DRAFT(4, "草稿"),
    REJECTED(5, "已驳回");

    private final int code;
    private final String label;

    QuestionStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static QuestionStatus fromCode(Integer code) {
        if (code != null) {
            for (QuestionStatus status : values()) {
                if (status.code == code) {
                    return status;
                }
            }
        }
        throw BizException.of(ErrorCode.PARAM_INVALID, "题目状态不合法");
    }
}
