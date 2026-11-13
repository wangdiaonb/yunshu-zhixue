package com.yunshu.zhixue.modules.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.interview.dto.InterviewStat;
import com.yunshu.zhixue.modules.interview.service.InterviewRecordService;
import com.yunshu.zhixue.modules.student.dto.ChangePasswordRequest;
import com.yunshu.zhixue.modules.student.dto.ProfileStats;
import com.yunshu.zhixue.modules.student.dto.ProfileStudy;
import com.yunshu.zhixue.modules.student.dto.ProfileUpdateRequest;
import com.yunshu.zhixue.modules.student.dto.StudentProfileResponse;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.student.service.StudentProfileService;
import com.yunshu.zhixue.modules.study.dto.PracticeStat;
import com.yunshu.zhixue.modules.study.service.StudyStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl implements StudentProfileService {

    private final StudentMapper studentMapper;
    private final PasswordEncoder passwordEncoder;
    private final StudyStatService studyStatService;
    private final InterviewRecordService interviewRecordService;

    @Override
    public StudentProfileResponse getProfile(Long studentId) {
        Student student = requireStudent(studentId);
        PracticeStat practice = studyStatService.statByStudents(List.of(studentId))
                .stream().findFirst().orElse(null);
        long interviewCount = interviewRecordService.countByStudents(List.of(studentId))
                .stream().mapToLong(InterviewStat::getInterviewCount).sum();
        long wrongQuestionCount = studyStatService.countWrongQuestions(studentId);

        ProfileStats stats = new ProfileStats(
                practice == null ? 0L : practice.getPracticeCount(),
                interviewCount,
                wrongQuestionCount);
        ProfileStudy study = new ProfileStudy(
                practice == null ? 0L : practice.getStudyDays(),
                practice == null ? 0.0 : practice.accuracy());

        return new StudentProfileResponse(
                student.getId(),
                student.getNickname(),
                maskMobile(student.getMobile()),
                student.getEmail(),
                student.getGender(),
                student.getPreferredSubjectId(),
                student.getAvatar(),
                student.getGuideFinished(),
                stats,
                study);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentProfileResponse updateProfile(Long studentId, ProfileUpdateRequest request) {
        requireStudent(studentId);
        LambdaUpdateWrapper<Student> update = Wrappers.lambdaUpdate();
        update.eq(Student::getId, studentId);
        boolean changed = false;

        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (!StringUtils.hasText(nickname)) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "请输入用户名");
            }
            update.set(Student::getNickname, nickname);
            changed = true;
        }

        if (request.email() != null) {
            String email = request.email().trim();
            if (!StringUtils.hasText(email)) {
                update.set(Student::getEmail, null);
            } else {
                long occupied = studentMapper.selectCount(Wrappers.<Student>lambdaQuery()
                        .eq(Student::getEmail, email)
                        .ne(Student::getId, studentId));
                if (occupied > 0) {
                    throw BizException.of(ErrorCode.CONTACT_OCCUPIED, "该邮箱已被其他学员使用");
                }
                update.set(Student::getEmail, email);
            }
            changed = true;
        }

        if (request.gender() != null) {
            update.set(Student::getGender, request.gender());
            changed = true;
        }

        if (request.preferredSubjectId() != null) {
            update.set(Student::getPreferredSubjectId, request.preferredSubjectId());
            changed = true;
        }

        if (request.avatar() != null) {
            String avatar = request.avatar().trim();
            if (!StringUtils.hasText(avatar)) {
                update.set(Student::getAvatar, null);
            } else if (!avatar.startsWith("/files/")) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "头像地址不合法");
            } else {
                update.set(Student::getAvatar, avatar);
            }
            changed = true;
        }

        if (!changed) {
            return getProfile(studentId);
        }

        try {
            studentMapper.update(null, update);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.CONTACT_OCCUPIED, "该邮箱已被其他学员使用");
        }
        return getProfile(studentId);
    }

    @Override
    public void changePassword(Long studentId, ChangePasswordRequest request) {
        Student student = requireStudent(studentId);
        String hash = student.getPasswordHash();
        if (!StringUtils.hasText(hash) || !hash.startsWith("$2")
                || !passwordEncoder.matches(request.oldPassword(), hash)) {
            throw BizException.of(ErrorCode.BAD_CREDENTIALS, "原密码不正确");
        }
        studentMapper.update(null, Wrappers.<Student>lambdaUpdate()
                .eq(Student::getId, studentId)
                .set(Student::getPasswordHash, passwordEncoder.encode(request.newPassword())));
    }

    @Override
    public void finishGuide(Long studentId) {
        requireStudent(studentId);
        studentMapper.update(null, Wrappers.<Student>lambdaUpdate()
                .eq(Student::getId, studentId)
                .set(Student::getGuideFinished, 1));
    }

    private Student requireStudent(Long studentId) {
        Student student = studentId == null ? null : studentMapper.selectById(studentId);
        if (student == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "学员不存在或已删除");
        }
        return student;
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(7);
    }
}
