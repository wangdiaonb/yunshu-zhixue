package com.yunshu.zhixue.modules.student.dto;

import java.time.LocalDateTime;

/**
 * 学员列表项（A-U01）。
 *
 * @param id             学员主键
 * @param nickname       昵称
 * @param avatar         头像 URL
 * @param mobile         手机号
 * @param email          邮箱
 * @param status         1 已启用 / 0 已禁用
 * @param createdAt      注册时间
 * @param lastLoginAt    最后登录
 * @param studyDays      学习天数（有刷题记录的自然日数）
 * @param courseCount    已学课程数（路线课程完成数）
 * @param practiceCount  刷题总数
 * @param accuracy       正确率（百分比，保留 1 位小数）
 * @param interviewCount 面试次数
 */
public record StudentItem(
        Long id,
        String nickname,
        String avatar,
        String mobile,
        String email,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        long studyDays,
        long courseCount,
        long practiceCount,
        double accuracy,
        long interviewCount
) {
}
