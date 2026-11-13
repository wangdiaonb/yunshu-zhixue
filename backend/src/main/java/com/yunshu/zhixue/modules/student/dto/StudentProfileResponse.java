package com.yunshu.zhixue.modules.student.dto;

/**
 * 学员个人信息（P-M01）：手机号脱敏只读，其余资料可编辑。
 */
public record StudentProfileResponse(
        Long id,
        String nickname,
        String mobile,
        String email,
        Integer gender,
        Long preferredSubjectId,
        String avatar,
        Integer guideFinished,
        ProfileStats stats,
        ProfileStudy study
) {
}
