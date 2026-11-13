package com.yunshu.zhixue.modules.student.dto;

/**
 * 个人中心累计数据（P-M01）。
 */
public record ProfileStats(long practiceCount, long interviewCount, long wrongQuestionCount) {
}
