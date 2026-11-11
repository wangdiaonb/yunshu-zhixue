package com.yunshu.zhixue.modules.route.service;

import com.yunshu.zhixue.modules.route.dto.RouteCard;
import com.yunshu.zhixue.modules.route.dto.RouteCourseOption;
import com.yunshu.zhixue.modules.route.dto.RouteDetail;
import com.yunshu.zhixue.modules.route.dto.RouteQuery;
import com.yunshu.zhixue.modules.route.dto.RouteSaveRequest;

import java.util.List;

/**
 * 学习路线管理服务（A-R01 ~ A-R09）。
 */
public interface RouteService {

    List<RouteCard> listRoutes(RouteQuery query);

    Long createRoute(RouteSaveRequest request);

    void updateRoute(Long routeId, RouteSaveRequest request);

    void updateStatus(Long routeId, Integer status);

    void deleteRoute(Long routeId);

    RouteDetail getDetail(Long routeId);

    /** 从课程库选择课程的候选项（排除已添加课程）。 */
    List<RouteCourseOption> listCourseOptions(Long routeId, String keyword);

    void addCourses(Long routeId, List<Long> courseIds);

    void removeCourse(Long routeId, Long courseId);

    /** orderedIds 为 route_course.id，按数组顺序重写学习顺序。 */
    void sortCourses(Long routeId, List<Long> orderedIds);
}
