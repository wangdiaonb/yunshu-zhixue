package com.yunshu.zhixue.modules.subject.dto;

/**
 * 学科管理统计卡（A-S01）。
 */
public record SubjectStatsResponse(long totalSubjects, long subjectsWithStages,
                                   long totalStages, long totalQuestions) {
}
