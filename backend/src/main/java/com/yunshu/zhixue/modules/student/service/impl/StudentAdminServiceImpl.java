package com.yunshu.zhixue.modules.student.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interview.dto.InterviewStat;
import com.yunshu.zhixue.modules.interview.service.InterviewRecordService;
import com.yunshu.zhixue.modules.route.dto.RouteCompletionStat;
import com.yunshu.zhixue.modules.route.service.RouteProgressService;
import com.yunshu.zhixue.modules.student.dto.StudentBatchIdsRequest;
import com.yunshu.zhixue.modules.student.dto.StudentBatchStatusRequest;
import com.yunshu.zhixue.modules.student.dto.StudentDetail;
import com.yunshu.zhixue.modules.student.dto.StudentItem;
import com.yunshu.zhixue.modules.student.dto.StudentQuery;
import com.yunshu.zhixue.modules.student.entity.Student;
import com.yunshu.zhixue.modules.student.mapper.StudentMapper;
import com.yunshu.zhixue.modules.student.service.StudentAdminService;
import com.yunshu.zhixue.modules.study.dto.PracticeStat;
import com.yunshu.zhixue.modules.study.service.StudyStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentAdminServiceImpl implements StudentAdminService {

    private final StudentMapper studentMapper;
    private final StudyStatService studyStatService;
    private final InterviewRecordService interviewRecordService;
    private final RouteProgressService routeProgressService;

    @Override
    public PageResult<StudentItem> pageStudents(StudentQuery query) {
        long pageNo = query.pageOrDefault();
        long pageSize = query.pageSizeOrDefault();

        LambdaQueryWrapper<Student> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(condition -> condition
                    .like(Student::getNickname, keyword)
                    .or()
                    .like(Student::getMobile, keyword));
        }
        if (query.getStatus() != null) {
            wrapper.eq(Student::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Student::getId);

        IPage<Student> result = studentMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<Student> students = result.getRecords();
        if (students.isEmpty()) {
            return PageResult.of(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }

        List<Long> ids = students.stream().map(Student::getId).toList();
        Map<Long, PracticeStat> practices =
                mapByStudent(studyStatService.statByStudents(ids), PracticeStat::getStudentId);
        Map<Long, InterviewStat> interviews =
                mapByStudent(interviewRecordService.countByStudents(ids), InterviewStat::getStudentId);
        Map<Long, RouteCompletionStat> completions =
                mapByStudent(routeProgressService.completedCountByStudents(ids), RouteCompletionStat::getStudentId);

        List<StudentItem> items = students.stream()
                .map(student -> {
                    PracticeStat practice = practices.get(student.getId());
                    InterviewStat interview = interviews.get(student.getId());
                    RouteCompletionStat completion = completions.get(student.getId());
                    return new StudentItem(
                            student.getId(),
                            student.getNickname(),
                            student.getAvatar(),
                            student.getMobile(),
                            student.getEmail(),
                            student.getStatus(),
                            student.getCreatedAt(),
                            student.getLastLoginAt(),
                            practice == null ? 0L : practice.getStudyDays(),
                            completion == null ? 0L : completion.getCourseCount(),
                            practice == null ? 0L : practice.getPracticeCount(),
                            practice == null ? 0.0 : practice.accuracy(),
                            interview == null ? 0L : interview.getInterviewCount());
                })
                .toList();
        return PageResult.of(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public StudentDetail getStudentDetail(Long studentId) {
        Student student = requireStudent(studentId);
        PracticeStat practice = studyStatService.statByStudents(List.of(studentId))
                .stream().findFirst().orElse(null);
        long interviewCount = interviewRecordService.countByStudents(List.of(studentId))
                .stream().mapToLong(InterviewStat::getInterviewCount).sum();
        long courseCount = routeProgressService.completedCountByStudents(List.of(studentId))
                .stream().mapToLong(RouteCompletionStat::getCourseCount).sum();
        return new StudentDetail(
                student.getId(),
                student.getNickname(),
                student.getAvatar(),
                student.getMobile(),
                student.getEmail(),
                student.getGender(),
                student.getStatus(),
                student.getCreatedAt(),
                student.getLastLoginAt(),
                practice == null ? 0L : practice.getStudyDays(),
                courseCount,
                practice == null ? 0L : practice.getPracticeCount(),
                practice == null ? 0L : practice.getCorrectCount(),
                practice == null ? 0.0 : practice.accuracy(),
                interviewCount);
    }

    @Override
    public void changeStatus(Long studentId, Integer status) {
        Student student = requireStudent(studentId);
        student.setStatus(status);
        studentMapper.updateById(student);
    }

    @Override
    public int batchChangeStatus(StudentBatchStatusRequest request) {
        List<Long> ids = normalizeIds(request.ids());
        if (ids.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "请选择学员");
        }
        return studentMapper.update(null, Wrappers.<Student>lambdaUpdate()
                .set(Student::getStatus, request.status())
                .in(Student::getId, ids));
    }

    @Override
    public void deleteStudent(Long studentId) {
        requireStudent(studentId);
        studentMapper.deleteById(studentId);
    }

    @Override
    public int batchDelete(StudentBatchIdsRequest request) {
        List<Long> ids = normalizeIds(request.ids());
        if (ids.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "请选择学员");
        }
        return studentMapper.delete(Wrappers.<Student>lambdaQuery().in(Student::getId, ids));
    }

    private Student requireStudent(Long studentId) {
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "学员不存在或已删除");
        }
        return student;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private <T> Map<Long, T> mapByStudent(List<T> stats, Function<T, Long> keyMapper) {
        if (stats == null || stats.isEmpty()) {
            return Map.of();
        }
        return stats.stream()
                .filter(item -> keyMapper.apply(item) != null)
                .collect(Collectors.toMap(keyMapper, Function.identity(), (left, right) -> left));
    }
}
