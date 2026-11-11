package com.yunshu.zhixue.modules.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.course.dto.CourseItem;
import com.yunshu.zhixue.modules.course.dto.CourseQuery;
import com.yunshu.zhixue.modules.course.dto.CourseSaveRequest;
import com.yunshu.zhixue.modules.course.dto.CourseStatsResponse;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.course.mapper.CourseSubjectMapper;
import com.yunshu.zhixue.modules.course.service.CourseService;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final Pattern BVID_PATTERN = Pattern.compile("BV[0-9A-Za-z]{10}");
    private static final String OTHER_SUBJECT = "其他学科";

    private final CourseMapper courseMapper;
    private final CourseSubjectMapper courseSubjectMapper;
    private final SubjectService subjectService;

    @Override
    public CourseStatsResponse stats() {
        long totalCourses = courseMapper.selectCount(null);
        long minutes = courseMapper.sumDurationMinutes();
        BigDecimal totalHours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        return new CourseStatsResponse(totalCourses, totalHours, courseMapper.countRelatedSubjects());
    }

    @Override
    public PageResult<CourseItem> pageCourses(CourseQuery query) {
        Page<Course> page = new Page<>(query.pageOrDefault(), query.pageSizeOrDefault());
        LambdaQueryWrapper<Course> wrapper = Wrappers.<Course>lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(condition -> condition
                    .like(Course::getName, keyword)
                    .or()
                    .like(Course::getBvid, keyword));
        }
        if (query.getSubjectId() != null) {
            Long subjectId = query.getSubjectId();
            if (subjectId == 0) {
                wrapper.isNull(Course::getSubjectId);
            } else {
                wrapper.and(condition -> condition
                        .eq(Course::getSubjectId, subjectId)
                        .or()
                        .inSql(Course::getId,
                                "SELECT course_id FROM course_subject WHERE delete_flag = 0 AND subject_id = "
                                        + subjectId));
            }
        }
        if (query.getSource() != null) {
            wrapper.eq(Course::getSource, query.getSource());
        }
        wrapper.orderByDesc(Course::getId);

        IPage<Course> result = courseMapper.selectPage(page, wrapper);
        List<Course> courses = result.getRecords();
        Map<Long, SubjectOption> subjectMap = subjectService.listOptions().stream()
                .collect(Collectors.toMap(SubjectOption::id, Function.identity(), (left, right) -> left));
        Map<Long, List<Long>> extraSubjectMap = loadExtraSubjects(courses.stream().map(Course::getId).toList());

        List<CourseItem> items = new ArrayList<>();
        for (Course course : courses) {
            List<Long> subjectIds = new ArrayList<>();
            if (course.getSubjectId() != null) {
                subjectIds.add(course.getSubjectId());
            }
            for (Long extraId : extraSubjectMap.getOrDefault(course.getId(), List.of())) {
                if (!subjectIds.contains(extraId)) {
                    subjectIds.add(extraId);
                }
            }
            List<String> subjectNames = subjectIds.stream()
                    .map(subjectMap::get)
                    .filter(java.util.Objects::nonNull)
                    .map(SubjectOption::name)
                    .toList();
            if (subjectNames.isEmpty()) {
                subjectNames = List.of(OTHER_SUBJECT);
            }
            String primaryName = course.getSubjectId() == null
                    ? OTHER_SUBJECT
                    : subjectMap.containsKey(course.getSubjectId())
                    ? subjectMap.get(course.getSubjectId()).name()
                    : OTHER_SUBJECT;
            items.add(new CourseItem(
                    course.getId(),
                    course.getName(),
                    course.getCoverUrl(),
                    course.getDurationMinutes(),
                    course.getSource(),
                    course.getLink(),
                    course.getBvid(),
                    course.getSubjectId(),
                    primaryName,
                    subjectIds,
                    subjectNames,
                    course.getCreatedAt()));
        }
        return PageResult.of(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(CourseSaveRequest request) {
        Course course = new Course();
        applyFields(course, request, null);
        try {
            courseMapper.insert(course);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "BV 号已存在");
        }
        insertRelations(course.getId(), resolveExtraSubjects(request));
        return course.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourse(Long courseId, CourseSaveRequest request) {
        Course course = requireCourse(courseId);
        applyFields(course, request, course);
        try {
            courseMapper.updateById(course);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "BV 号已存在");
        }
        courseSubjectMapper.deletePhysicalByCourseId(courseId);
        insertRelations(courseId, resolveExtraSubjects(request));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long courseId) {
        Course course = requireCourse(courseId);
        if (courseMapper.countRouteReferences(courseId) > 0) {
            throw BizException.of(ErrorCode.CONTENT_REFERENCED,
                    "课程「" + course.getName() + "」已被学习路线引用，无法删除");
        }
        courseMapper.deleteById(courseId);
        courseSubjectMapper.deletePhysicalByCourseId(courseId);
    }

    private Course requireCourse(Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return course;
    }

    private void applyFields(Course course, CourseSaveRequest request, Course existing) {
        String name = request.name().trim();
        Long subjectId = normalizeSubjectId(request.subjectId());
        Set<Long> validSubjectIds = subjectService.listOptions().stream()
                .map(SubjectOption::id)
                .collect(Collectors.toSet());
        if (subjectId != null && !validSubjectIds.contains(subjectId)) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "所属学科不存在");
        }
        for (Long extraId : normalizeExtras(request.subjectIds(), subjectId)) {
            if (!validSubjectIds.contains(extraId)) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "关联学科不存在");
            }
        }

        String link = trimToNull(request.link());
        String bvid = trimToNull(request.bvid());
        if (!StringUtils.hasText(bvid)) {
            bvid = extractBvid(link);
        }
        if (!StringUtils.hasText(bvid) && existing != null) {
            bvid = existing.getBvid();
        }
        if (StringUtils.hasText(bvid)) {
            if (bvid.length() > 20) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "BV 号格式不正确");
            }
            Long duplicate = courseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                    .eq(Course::getBvid, bvid)
                    .ne(existing != null, Course::getId, existing == null ? null : existing.getId()));
            if (duplicate != null && duplicate > 0) {
                throw BizException.of(ErrorCode.DUPLICATE_NAME, "BV 号已存在");
            }
        }

        course.setName(name);
        course.setCoverUrl(trimToNull(request.coverUrl()));
        course.setDurationMinutes(request.durationMinutes());
        course.setSource(request.source());
        course.setLink(link);
        course.setBvid(bvid);
        course.setSubjectId(subjectId);
    }

    private List<Long> resolveExtraSubjects(CourseSaveRequest request) {
        Long main = normalizeSubjectId(request.subjectId());
        return normalizeExtras(request.subjectIds(), main);
    }

    private List<Long> normalizeExtras(List<Long> subjectIds, Long mainSubjectId) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return List.of();
        }
        Set<Long> extras = new LinkedHashSet<>();
        for (Long id : subjectIds) {
            if (id != null && id > 0 && !id.equals(mainSubjectId)) {
                extras.add(id);
            }
        }
        return new ArrayList<>(extras);
    }

    private Long normalizeSubjectId(Long subjectId) {
        return subjectId == null || subjectId == 0 ? null : subjectId;
    }

    private void insertRelations(Long courseId, List<Long> extraSubjectIds) {
        for (Long subjectId : extraSubjectIds) {
            CourseSubject relation = new CourseSubject();
            relation.setCourseId(courseId);
            relation.setSubjectId(subjectId);
            courseSubjectMapper.insert(relation);
        }
    }

    private Map<Long, List<Long>> loadExtraSubjects(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        List<CourseSubject> relations = courseSubjectMapper.selectList(Wrappers.<CourseSubject>lambdaQuery()
                .in(CourseSubject::getCourseId, courseIds));
        Map<Long, List<Long>> result = new HashMap<>();
        for (CourseSubject relation : relations) {
            result.computeIfAbsent(relation.getCourseId(), key -> new ArrayList<>())
                    .add(relation.getSubjectId());
        }
        return result;
    }

    private String extractBvid(String link) {
        if (!StringUtils.hasText(link)) {
            return null;
        }
        Matcher matcher = BVID_PATTERN.matcher(link);
        return matcher.find() ? matcher.group() : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
