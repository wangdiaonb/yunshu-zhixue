package com.yunshu.zhixue.modules.student.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interview.dto.InterviewRecordItem;
import com.yunshu.zhixue.modules.interview.service.InterviewRecordService;
import com.yunshu.zhixue.modules.student.dto.StudentBatchIdsRequest;
import com.yunshu.zhixue.modules.student.dto.StudentBatchStatusRequest;
import com.yunshu.zhixue.modules.student.dto.StudentDetail;
import com.yunshu.zhixue.modules.student.dto.StudentItem;
import com.yunshu.zhixue.modules.student.dto.StudentQuery;
import com.yunshu.zhixue.modules.student.dto.StudentStatusRequest;
import com.yunshu.zhixue.modules.student.service.StudentAdminService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端学员管理接口（A-U01 ~ A-U07）。
 *
 * <p>学员由前台注册产生，管理端不提供新增与编辑（需求 4.4），
 * 也不提供导出（评审 R-02 裁决）。</p>
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/students")
@RequiredArgsConstructor
@Tag(name = "管理端-学员管理")
public class StudentAdminController {

    private final StudentAdminService studentAdminService;
    private final InterviewRecordService interviewRecordService;

    @GetMapping
    @RequirePermission("student:view")
    @Operation(summary = "学员分页列表")
    public ApiResponse<PageResult<StudentItem>> page(@Valid StudentQuery query) {
        return ApiResponse.ok(studentAdminService.pageStudents(query));
    }

    @GetMapping("/{id}/detail")
    @RequirePermission("student:view")
    @Operation(summary = "学员详情与学习统计")
    public ApiResponse<StudentDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(studentAdminService.getStudentDetail(id));
    }

    @GetMapping("/{id}/interviews")
    @RequirePermission("student:view")
    @Operation(summary = "学员面试记录")
    public ApiResponse<PageResult<InterviewRecordItem>> interviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        long pageNo = Math.max(1, page);
        long size = Math.min(100, Math.max(1, pageSize));
        return ApiResponse.ok(interviewRecordService.pageByStudent(id, pageNo, size));
    }

    @PutMapping("/{id}/status")
    @RequirePermission("student:edit")
    @OperationLog(action = OperationAction.STATUS_CHANGE, targetType = "student", targetName = "#id")
    @Operation(summary = "启用/禁用学员")
    public ApiResponse<Void> changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody StudentStatusRequest request) {
        studentAdminService.changeStatus(id, request.status());
        return ApiResponse.ok();
    }

    @PutMapping("/batch-status")
    @RequirePermission("student:edit")
    @OperationLog(action = OperationAction.STATUS_CHANGE, targetType = "student", targetName = "批量更新学员状态")
    @Operation(summary = "批量启用/禁用学员")
    public ApiResponse<Integer> batchStatus(@Valid @RequestBody StudentBatchStatusRequest request) {
        return ApiResponse.ok(studentAdminService.batchChangeStatus(request));
    }

    @DeleteMapping("/{id}")
    @RequirePermission("student:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "student", targetName = "#id")
    @Operation(summary = "删除学员（逻辑删除）")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        studentAdminService.deleteStudent(id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/batch")
    @RequirePermission("student:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "student", targetName = "批量删除学员")
    @Operation(summary = "批量删除学员（逻辑删除）")
    public ApiResponse<Integer> batchDelete(@Valid @RequestBody StudentBatchIdsRequest request) {
        return ApiResponse.ok(studentAdminService.batchDelete(request));
    }
}
