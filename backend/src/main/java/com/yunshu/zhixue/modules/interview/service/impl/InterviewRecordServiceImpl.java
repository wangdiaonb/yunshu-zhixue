package com.yunshu.zhixue.modules.interview.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interview.dto.InterviewRecordItem;
import com.yunshu.zhixue.modules.interview.dto.InterviewStat;
import com.yunshu.zhixue.modules.interview.mapper.InterviewRecordMapper;
import com.yunshu.zhixue.modules.interview.service.InterviewRecordService;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewRecordServiceImpl implements InterviewRecordService {

    private final InterviewRecordMapper interviewRecordMapper;
    private final SubjectService subjectService;

    @Override
    public List<InterviewStat> countByStudents(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = studentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return interviewRecordMapper.countByStudents(ids);
    }

    @Override
    public PageResult<InterviewRecordItem> pageByStudent(Long studentId, long page, long pageSize) {
        IPage<InterviewRecordItem> result =
                interviewRecordMapper.selectPageByStudent(new Page<>(page, pageSize), studentId);
        List<InterviewRecordItem> items = result.getRecords();
        if (!items.isEmpty()) {
            Map<Long, String> subjectNames = subjectService.listOptions().stream()
                    .collect(Collectors.toMap(SubjectOption::id, SubjectOption::name, (left, right) -> left));
            items.forEach(item -> item.setSubjectName(
                    item.getSubjectId() == null ? null : subjectNames.get(item.getSubjectId())));
        }
        return PageResult.of(items, result.getTotal(), result.getCurrent(), result.getSize());
    }
}
