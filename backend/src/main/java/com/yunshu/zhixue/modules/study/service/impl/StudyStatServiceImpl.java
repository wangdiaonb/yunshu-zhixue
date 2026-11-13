package com.yunshu.zhixue.modules.study.service.impl;

import com.yunshu.zhixue.modules.study.dto.PracticeStat;
import com.yunshu.zhixue.modules.study.mapper.PracticeRecordMapper;
import com.yunshu.zhixue.modules.study.mapper.WrongQuestionMapper;
import com.yunshu.zhixue.modules.study.service.StudyStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class StudyStatServiceImpl implements StudyStatService {

    private final PracticeRecordMapper practiceRecordMapper;
    private final WrongQuestionMapper wrongQuestionMapper;

    @Override
    public List<PracticeStat> statByStudents(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = studentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return practiceRecordMapper.statByStudents(ids);
    }

    @Override
    public long countWrongQuestions(Long studentId) {
        return studentId == null ? 0L : wrongQuestionMapper.countByStudentId(studentId);
    }
}
