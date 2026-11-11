package com.yunshu.zhixue.modules.student.dto;

import java.time.LocalDateTime;

/**
 * 学员详情（A-U06）：资料 + 学习统计 + 面试累计次数。
 */
public record StudentDetail(
        Long id,
        String nickname,
        String avatar,
        String mobile,
        String email,
        Integer gender,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        long studyDays,
        long courseCount,
        long practiceCount,
        long correctCount,
        double accuracy,
        long interviewCount
) {
}
