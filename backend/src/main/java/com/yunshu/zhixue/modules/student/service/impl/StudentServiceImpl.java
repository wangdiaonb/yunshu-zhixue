package com.yunshu.zhixue.modules.student.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.modules.student.dto.StudentAccount;
import com.yunshu.zhixue.modules.student.dto.StudentSummary;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;

    @Override
    public StudentAccount findAccountForLogin(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        String value = account.trim();

        Student student = studentMapper.selectOne(Wrappers.<Student>lambdaQuery()
                .eq(Student::getMobile, value)
                .last("LIMIT 1"));
        if (student == null) {
            List<Student> matches = studentMapper.selectList(Wrappers.<Student>lambdaQuery()
                    .eq(Student::getEmail, value));
            if (matches.size() != 1) {
                return null;
            }
            student = matches.get(0);
        }
        return toAccount(student);
    }

    @Override
    public StudentAccount findAccountById(Long studentId) {
        if (studentId == null) {
            return null;
        }
        return toAccount(studentMapper.selectById(studentId));
    }

    @Override
    public StudentAccount findAccountByMobile(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return null;
        }
        Student student = studentMapper.selectOne(Wrappers.<Student>lambdaQuery()
                .eq(Student::getMobile, mobile.trim())
                .last("LIMIT 1"));
        return toAccount(student);
    }

    @Override
    public boolean existsByMobile(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return false;
        }
        return studentMapper.selectCount(Wrappers.<Student>lambdaQuery()
                .eq(Student::getMobile, mobile.trim())) > 0;
    }

    @Override
    public StudentAccount createStudent(String mobile, String passwordHash, String nickname) {
        Student student = new Student();
        student.setMobile(mobile.trim());
        student.setPasswordHash(passwordHash);
        student.setNickname(nickname);
        student.setGender(0);
        student.setGuideFinished(0);
        student.setStatus(1);
        studentMapper.insert(student);
        return toAccount(student);
    }

    @Override
    public void updatePasswordHash(Long studentId, String passwordHash) {
        studentMapper.update(null, Wrappers.<Student>lambdaUpdate()
                .eq(Student::getId, studentId)
                .set(Student::getPasswordHash, passwordHash));
    }

    @Override
    public StudentSummary getSummary(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        return student == null ? null
                : new StudentSummary(student.getId(), student.getNickname(), student.getAvatar());
    }

    @Override
    public void recordLogin(Long studentId) {
        studentMapper.update(null, Wrappers.<Student>lambdaUpdate()
                .eq(Student::getId, studentId)
                .set(Student::getLastLoginAt, LocalDateTime.now(ZoneOffset.UTC)));
    }

    private StudentAccount toAccount(Student student) {
        if (student == null) {
            return null;
        }
        return new StudentAccount(
                student.getId(),
                student.getMobile(),
                student.getEmail(),
                student.getPasswordHash(),
                student.getNickname(),
                student.getAvatar(),
                student.getStatus() == null ? 0 : student.getStatus());
    }
}
