package com.yunshu.zhixue.modules.route.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.route.dto.RouteCard;
import com.yunshu.zhixue.modules.route.dto.RouteCourseItem;
import com.yunshu.zhixue.modules.route.dto.RouteCourseOption;
import com.yunshu.zhixue.modules.route.dto.RouteDetail;
import com.yunshu.zhixue.modules.route.dto.RoutePreviewCourse;
import com.yunshu.zhixue.modules.route.dto.RouteQuery;
import com.yunshu.zhixue.modules.route.dto.RouteSaveRequest;
import com.yunshu.zhixue.modules.route.entity.LearningRoute;
import com.yunshu.zhixue.modules.route.entity.RouteCourse;
import com.yunshu.zhixue.modules.route.mapper.LearningRouteMapper;
import com.yunshu.zhixue.modules.route.mapper.RouteCourseMapper;
import com.yunshu.zhixue.modules.route.service.RouteService;
import com.yunshu.zhixue.modules.route.support.RouteCourseRow;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private static final String OTHER_SUBJECT = "其他学科";

    private final LearningRouteMapper routeMapper;
    private final RouteCourseMapper routeCourseMapper;
    private final CourseMapper courseMapper;
    private final SubjectService subjectService;

    @Override
    public List<RouteCard> listRoutes(RouteQuery query) {
        LambdaQueryWrapper<LearningRoute> wrapper = Wrappers.<LearningRoute>lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(condition -> condition
                    .like(LearningRoute::getName, keyword)
                    .or()
                    .like(LearningRoute::getDescription, keyword));
        }
        if (query.getSubjectId() != null) {
            wrapper.eq(LearningRoute::getSubjectId, query.getSubjectId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(LearningRoute::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(LearningRoute::getId);

        List<LearningRoute> routes = routeMapper.selectList(wrapper);
        Map<Long, SubjectOption> subjectMap = subjectMap();
        List<RouteCard> cards = new ArrayList<>();
        for (LearningRoute route : routes) {
            SubjectOption subject = subjectMap.get(route.getSubjectId());
            List<RoutePreviewCourse> previews = routeCourseMapper.selectPreviewRows(route.getId(), 4).stream()
                    .map(row -> new RoutePreviewCourse(
                            row.getRouteCourseId(),
                            row.getCourseId(),
                            row.getName(),
                            row.getBvid(),
                            row.getCoverUrl(),
                            row.getDurationMinutes(),
                            row.getSortOrder()))
                    .toList();
            cards.add(new RouteCard(
                    route.getId(),
                    route.getName(),
                    route.getSubjectId(),
                    subject == null ? "未知学科" : subject.name(),
                    subject == null ? "📚" : subject.icon(),
                    route.getIcon(),
                    route.getTargetEducation(),
                    route.getDescription(),
                    route.getStatus(),
                    routeCourseMapper.countByRoute(route.getId()),
                    routeCourseMapper.sumDurationByRoute(route.getId()),
                    previews));
        }
        return cards;
    }

    @Override
    public Long createRoute(RouteSaveRequest request) {
        SubjectOption subject = requireSubject(request.subjectId());
        LearningRoute route = new LearningRoute();
        route.setName(request.name().trim());
        route.setSubjectId(subject.id());
        route.setTargetEducation(trimToNull(request.targetEducation()));
        route.setIcon(trimToNull(request.icon()));
        route.setDescription(trimToNull(request.description()));
        route.setStatus(request.status() == null ? 1 : request.status());
        routeMapper.insert(route);
        return route.getId();
    }

    @Override
    public void updateRoute(Long routeId, RouteSaveRequest request) {
        LearningRoute route = requireRoute(routeId);
        SubjectOption subject = requireSubject(request.subjectId());
        route.setName(request.name().trim());
        route.setSubjectId(subject.id());
        route.setTargetEducation(trimToNull(request.targetEducation()));
        route.setIcon(trimToNull(request.icon()));
        route.setDescription(trimToNull(request.description()));
        if (request.status() != null) {
            route.setStatus(request.status());
        }
        routeMapper.updateById(route);
    }

    @Override
    public void updateStatus(Long routeId, Integer status) {
        requireRoute(routeId);
        routeMapper.update(null, Wrappers.<LearningRoute>lambdaUpdate()
                .eq(LearningRoute::getId, routeId)
                .set(LearningRoute::getStatus, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoute(Long routeId) {
        requireRoute(routeId);
        routeCourseMapper.deletePhysicalByRoute(routeId);
        routeMapper.deleteById(routeId);
    }

    @Override
    public RouteDetail getDetail(Long routeId) {
        LearningRoute route = requireRoute(routeId);
        Map<Long, SubjectOption> subjectMap = subjectMap();
        SubjectOption subject = subjectMap.get(route.getSubjectId());
        List<RouteCourseItem> courses = routeCourseMapper
                .selectCourseRows(routeId, route.getSubjectId())
                .stream()
                .map(row -> toCourseItem(row, subjectMap))
                .toList();
        int courseCount = routeCourseMapper.countByRoute(routeId);
        long totalDuration = routeCourseMapper.sumDurationByRoute(routeId);
        return new RouteDetail(
                route.getId(),
                route.getName(),
                route.getSubjectId(),
                subject == null ? "未知学科" : subject.name(),
                subject == null ? "📚" : subject.icon(),
                route.getIcon(),
                route.getTargetEducation(),
                route.getDescription(),
                route.getStatus(),
                courseCount,
                totalDuration,
                courses);
    }

    @Override
    public List<RouteCourseOption> listCourseOptions(Long routeId, String keyword) {
        LearningRoute route = requireRoute(routeId);
        String search = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Map<Long, SubjectOption> subjectMap = subjectMap();
        return routeCourseMapper.selectAvailableCourses(routeId, route.getSubjectId(), search).stream()
                .map(row -> new RouteCourseOption(
                        row.getCourseId(),
                        row.getName(),
                        row.getBvid(),
                        row.getCoverUrl(),
                        row.getDurationMinutes(),
                        row.getSource(),
                        row.getSubjectId(),
                        subjectName(subjectMap, row.getSubjectId()),
                        isLocal(row)))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addCourses(Long routeId, List<Long> courseIds) {
        LearningRoute route = requireRoute(routeId);
        Set<Long> requested = courseIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "请选择要添加的课程");
        }
        List<Course> courses = courseMapper.selectBatchIds(requested);
        if (courses.isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "所选课程不存在或已删除");
        }
        Set<Long> existing = new LinkedHashSet<>(routeCourseMapper.selectCourseIds(routeId));
        int sort = routeCourseMapper.maxSortOrder(routeId);
        int added = 0;
        for (Course course : courses) {
            if (existing.contains(course.getId())) {
                continue;
            }
            RouteCourse relation = new RouteCourse();
            relation.setRouteId(routeId);
            relation.setCourseId(course.getId());
            relation.setSortOrder(++sort);
            routeCourseMapper.insert(relation);
            existing.add(course.getId());
            added++;
        }
        if (added == 0) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "所选课程已全部在路线中");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCourse(Long routeId, Long courseId) {
        requireRoute(routeId);
        int removed = routeCourseMapper.deletePhysicalByCourse(routeId, courseId);
        if (removed == 0) {
            throw BizException.of(ErrorCode.NOT_FOUND, "路线中不存在该课程");
        }
        normalizeSort(routeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortCourses(Long routeId, List<Long> orderedIds) {
        requireRoute(routeId);
        List<Long> routeCourseIds = routeCourseMapper.selectRouteCourseIds(routeId);
        Set<Long> expected = new LinkedHashSet<>(routeCourseIds);
        Set<Long> provided = new LinkedHashSet<>(orderedIds);
        if (provided.size() != orderedIds.size() || !Objects.equals(expected, provided)) {
            throw BizException.of(ErrorCode.PARAM_INVALID,
                    "排序列表与当前路线课程不一致，请刷新后重试");
        }
        for (int index = 0; index < orderedIds.size(); index++) {
            routeCourseMapper.update(null, Wrappers.<RouteCourse>lambdaUpdate()
                    .eq(RouteCourse::getId, orderedIds.get(index))
                    .set(RouteCourse::getSortOrder, index + 1));
        }
    }

    private RouteCourseItem toCourseItem(RouteCourseRow row, Map<Long, SubjectOption> subjectMap) {
        return new RouteCourseItem(
                row.getRouteCourseId(),
                row.getCourseId(),
                row.getName(),
                row.getBvid(),
                row.getCoverUrl(),
                row.getDurationMinutes(),
                row.getSource(),
                row.getSortOrder(),
                row.getSubjectId(),
                subjectName(subjectMap, row.getSubjectId()),
                isLocal(row));
    }

    private void normalizeSort(Long routeId) {
        List<Long> ids = routeCourseMapper.selectRouteCourseIds(routeId);
        for (int index = 0; index < ids.size(); index++) {
            routeCourseMapper.update(null, Wrappers.<RouteCourse>lambdaUpdate()
                    .eq(RouteCourse::getId, ids.get(index))
                    .set(RouteCourse::getSortOrder, index + 1));
        }
    }

    private LearningRoute requireRoute(Long routeId) {
        LearningRoute route = routeMapper.selectById(routeId);
        if (route == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "学习路线不存在");
        }
        return route;
    }

    private SubjectOption requireSubject(Long subjectId) {
        return subjectService.listOptions().stream()
                .filter(option -> option.id().equals(subjectId))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.PARAM_INVALID, "关联学科不存在"));
    }

    private Map<Long, SubjectOption> subjectMap() {
        return subjectService.listOptions().stream()
                .collect(Collectors.toMap(SubjectOption::id, Function.identity(), (left, right) -> left));
    }

    private String subjectName(Map<Long, SubjectOption> subjectMap, Long subjectId) {
        if (subjectId == null) {
            return OTHER_SUBJECT;
        }
        SubjectOption option = subjectMap.get(subjectId);
        return option == null ? OTHER_SUBJECT : option.name();
    }

    private boolean isLocal(RouteCourseRow row) {
        return row.getLocalSubject() != null && row.getLocalSubject() == 1;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
