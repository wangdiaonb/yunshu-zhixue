package com.yunshu.zhixue.modules.subject.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.subject.dto.StageSaveRequest;
import com.yunshu.zhixue.modules.subject.dto.StageStatusRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectCard;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.dto.SubjectSaveRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectSortRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectStatsResponse;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端学科与阶段管理接口（A-S01 ~ A-S11）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/subjects")
@RequiredArgsConstructor
@Tag(name = "管理端-学科管理")
public class SubjectAdminController {

    private final SubjectService subjectService;

    @GetMapping("/stats")
    @RequirePermission("subject:view")
    @Operation(summary = "学科统计卡")
    public ApiResponse<SubjectStatsResponse> stats() {
        return ApiResponse.ok(subjectService.stats());
    }

    @GetMapping
    @RequirePermission("subject:view")
    @Operation(summary = "学科卡片列表（含阶段）")
    public ApiResponse<List<SubjectCard>> list() {
        return ApiResponse.ok(subjectService.listSubjects());
    }

    @GetMapping("/options")
    @RequirePermission("subject:view")
    @Operation(summary = "学科下拉选项")
    public ApiResponse<List<SubjectOption>> options() {
        return ApiResponse.ok(subjectService.listOptions());
    }

    @PostMapping
    @RequirePermission("subject:add")
    @OperationLog(action = OperationAction.CREATE, targetType = "subject", targetName = "#request.name")
    @Operation(summary = "新增学科")
    public ApiResponse<Long> create(@Valid @RequestBody SubjectSaveRequest request) {
        return ApiResponse.ok(subjectService.createSubject(request));
    }

    @PutMapping("/sort")
    @RequirePermission("subject:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "subject", targetName = "学科排序")
    @Operation(summary = "调整学科顺序")
    public ApiResponse<Void> sort(@Valid @RequestBody SubjectSortRequest request) {
        subjectService.sortSubjects(request.orderedIds());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    @RequirePermission("subject:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "subject", targetName = "#request.name")
    @Operation(summary = "编辑学科")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody SubjectSaveRequest request) {
        subjectService.updateSubject(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("subject:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "subject", targetName = "#id")
    @Operation(summary = "删除学科")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{subjectId}/stages")
    @RequirePermission("subject:add")
    @OperationLog(action = OperationAction.CREATE, targetType = "stage", targetName = "#request.name")
    @Operation(summary = "新增阶段")
    public ApiResponse<Long> createStage(@PathVariable Long subjectId,
                                         @Valid @RequestBody StageSaveRequest request) {
        return ApiResponse.ok(subjectService.createStage(subjectId, request));
    }

    @PutMapping("/{subjectId}/stages/sort")
    @RequirePermission("subject:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "stage", targetName = "阶段排序")
    @Operation(summary = "调整阶段顺序")
    public ApiResponse<Void> sortStages(@PathVariable Long subjectId,
                                        @Valid @RequestBody SubjectSortRequest request) {
        subjectService.sortStages(subjectId, request.orderedIds());
        return ApiResponse.ok();
    }

    @PutMapping("/{subjectId}/stages/{id}")
    @RequirePermission("subject:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "stage", targetName = "#request.name")
    @Operation(summary = "编辑阶段")
    public ApiResponse<Void> updateStage(@PathVariable Long subjectId, @PathVariable Long id,
                                         @Valid @RequestBody StageSaveRequest request) {
        subjectService.updateStage(subjectId, id, request);
        return ApiResponse.ok();
    }

    @PutMapping("/{subjectId}/stages/{id}/status")
    @RequirePermission("subject:edit")
    @OperationLog(action = OperationAction.STATUS_CHANGE, targetType = "stage", targetName = "#id")
    @Operation(summary = "启用/停用阶段")
    public ApiResponse<Void> updateStageStatus(@PathVariable Long subjectId, @PathVariable Long id,
                                               @Valid @RequestBody StageStatusRequest request) {
        subjectService.updateStageStatus(subjectId, id, request.status());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{subjectId}/stages/{id}")
    @RequirePermission("subject:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "stage", targetName = "#id")
    @Operation(summary = "删除阶段")
    public ApiResponse<Void> deleteStage(@PathVariable Long subjectId, @PathVariable Long id) {
        subjectService.deleteStage(subjectId, id);
        return ApiResponse.ok();
    }
}
