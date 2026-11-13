package com.yunshu.zhixue.modules.student.service;

import com.yunshu.zhixue.modules.student.dto.ChangePasswordRequest;
import com.yunshu.zhixue.modules.student.dto.ProfileUpdateRequest;
import com.yunshu.zhixue.modules.student.dto.StudentProfileResponse;

/**
 * 学员个人中心（P-M01 ~ P-M04）。
 */
public interface StudentProfileService {

    StudentProfileResponse getProfile(Long studentId);

    StudentProfileResponse updateProfile(Long studentId, ProfileUpdateRequest request);

    void changePassword(Long studentId, ChangePasswordRequest request);

    void finishGuide(Long studentId);
}
