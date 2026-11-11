package com.yunshu.zhixue.modules.route.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.route.dto.RouteCard;
import com.yunshu.zhixue.modules.route.dto.RouteCourseAddRequest;
import com.yunshu.zhixue.modules.route.dto.RouteCourseOption;
import com.yunshu.zhixue.modules.route.dto.RouteCourseSortRequest;
import com.yunshu.zhixue.modules.route.dto.RouteDetail;
import com.yunshu.zhixue.modules.route.dto.RouteQuery;
import com.yunshu.zhixue.modules.route.dto.RouteSaveRequest;
import com.yunshu.zhixue.modules.route.dto.RouteStatusRequest;
import com.yunshu.zhixue.modules.route.service.RouteService;
import com.yunshu.zhixue.security.RequirePermission;
import com.yunshu.zhixue.support.log.OperationAction;
import com.yunshu.zhixue.support.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端学习路线接口（A-R01 ~ A-R09）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/routes")
@RequiredArgsConstructor
@Tag(name = "管理端-学习路线")
public class RouteAdminController {

    private final RouteService routeService;

    @GetMapping
    @RequirePermission("route:view")
    @Operation(summary = "学习路线卡片列表")
    public ApiResponse<List<RouteCard>> list(@Valid RouteQuery query) {
        return ApiResponse.ok(routeService.listRoutes(query));
    }

    @PostMapping
    @RequirePermission("route:add")
    @OperationLog(action = OperationAction.CREATE, targetType = "route", targetName = "#request.name")
    @Operation(summary = "新建学习路线")
    public ApiResponse<Long> create(@Valid @RequestBody RouteSaveRequest request) {
        return ApiResponse.ok(routeService.createRoute(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("route:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "route", targetName = "#request.name")
    @Operation(summary = "编辑学习路线")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody RouteSaveRequest request) {
        routeService.updateRoute(id, request);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/status")
    @RequirePermission("route:edit")
    @OperationLog(action = OperationAction.STATUS_CHANGE, targetType = "route", targetName = "#id")
    @Operation(summary = "发布/下架学习路线")
    public ApiResponse<Void> updateStatus(@PathVariable Long id,
                                          @Valid @RequestBody RouteStatusRequest request) {
        routeService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("route:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "route", targetName = "#id")
    @Operation(summary = "删除学习路线")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/detail")
    @RequirePermission("route:view")
    @Operation(summary = "学习路线详情/预览")
    public ApiResponse<RouteDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(routeService.getDetail(id));
    }

    @GetMapping("/{id}/course-options")
    @RequirePermission("route:edit")
    @Operation(summary = "从课程库添加课程的候选项")
    public ApiResponse<List<RouteCourseOption>> courseOptions(
            @PathVariable Long id,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(routeService.listCourseOptions(id, keyword));
    }

    @PostMapping("/{id}/courses")
    @RequirePermission("route:edit")
    @OperationLog(action = OperationAction.CREATE, targetType = "route", targetName = "路线课程")
    @Operation(summary = "从课程库添加课程")
    public ApiResponse<Void> addCourses(@PathVariable Long id,
                                        @Valid @RequestBody RouteCourseAddRequest request) {
        routeService.addCourses(id, request.courseIds());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}/courses/{courseId}")
    @RequirePermission("route:edit")
    @OperationLog(action = OperationAction.DELETE, targetType = "route", targetName = "#courseId")
    @Operation(summary = "移除路线课程")
    public ApiResponse<Void> removeCourse(@PathVariable Long id, @PathVariable Long courseId) {
        routeService.removeCourse(id, courseId);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/courses/sort")
    @RequirePermission("route:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "route", targetName = "路线课程排序")
    @Operation(summary = "调整路线课程顺序")
    public ApiResponse<Void> sortCourses(@PathVariable Long id,
                                         @Valid @RequestBody RouteCourseSortRequest request) {
        routeService.sortCourses(id, request.orderedIds());
        return ApiResponse.ok();
    }
}
