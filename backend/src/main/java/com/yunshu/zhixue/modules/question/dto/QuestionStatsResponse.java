package com.yunshu.zhixue.modules.question.dto;

/**
 * 题库统计卡（A-Q01）。
 */
public record QuestionStatsResponse(long total, long published, long pending,
                                    long single, long multiple, long judge, long shortAnswer) {
}
