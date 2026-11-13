package com.yunshu.zhixue.modules.student.dto;

/**
 * 个人中心学习数据（P-M01）。
 *
 * @param totalDays    有练习记录的天数（Asia/Shanghai 自然日）
 * @param accuracyRate 正确率（0~100，保留 1 位小数）
 */
public record ProfileStudy(long totalDays, double accuracyRate) {
}
