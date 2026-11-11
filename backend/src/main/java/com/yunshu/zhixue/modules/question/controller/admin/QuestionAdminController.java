package com.yunshu.zhixue.modules.question.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.dto.QuestionAuditRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchDeleteResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchIdsRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchStatusRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchStatusResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionDeleteResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionDetail;
import com.yunshu.zhixue.modules.question.dto.QuestionImportConfirmRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionImportConfirmResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPreviewResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionItem;
import com.yunshu.zhixue.modules.question.dto.QuestionQuery;
import com.yunshu.zhixue.modules.question.dto.QuestionSaveRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionStatsResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionStatusRequest;
import com.yunshu.zhixue.modules.question.service.QuestionImportService;
import com.yunshu.zhixue.modules.question.service.QuestionService;
import com.yunshu.zhixue.security.RequirePermission;
import com.yunshu.zhixue.support.log.OperationAction;
import com.yunshu.zhixue.support.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 管理端题库接口（A-Q01 ~ A-Q15）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/questions")
@RequiredArgsConstructor
@Tag(name = "管理端-题库管理")
public class QuestionAdminController {

    private final QuestionService questionService;
    private final QuestionImportService questionImportService;

    @GetMapping("/stats")
    @RequirePermission("question:view")
    @Operation(summary = "题库统计卡")
    public ApiResponse<QuestionStatsResponse> stats() {
        return ApiResponse.ok(questionService.stats());
    }

    @GetMapping
    @RequirePermission("question:view")
    @Operation(summary = "题目分页列表")
    public ApiResponse<PageResult<QuestionItem>> page(@Valid QuestionQuery query) {
        return ApiResponse.ok(questionService.pageQuestions(query));
    }

    @GetMapping("/{id}")
    @RequirePermission("question:view")
    @Operation(summary = "题目详情")
    public ApiResponse<QuestionDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(questionService.getQuestion(id));
    }

    @PostMapping
    @RequirePermission("question:add")
    @OperationLog(action = OperationAction.CREATE, targetType = "question", targetName = "题目")
    @Operation(summary = "新增题目")
    public ApiResponse<Long> create(@Valid @RequestBody QuestionSaveRequest request) {
        return ApiResponse.ok(questionService.createQuestion(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("question:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "question", targetName = "题目")
    @Operation(summary = "编辑题目")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody QuestionSaveRequest request) {
        questionService.updateQuestion(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("question:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "question", targetName = "题目")
    @Operation(summary = "删除题目")
    public ApiResponse<QuestionDeleteResponse> delete(@PathVariable Long id) {
        return ApiResponse.ok(questionService.deleteQuestion(id));
    }

    @PostMapping("/{id}/audit")
    @RequirePermission("question:audit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "question", targetName = "题目审核")
    @Operation(summary = "审核题目")
    public ApiResponse<Void> audit(@PathVariable Long id,
                                   @Valid @RequestBody QuestionAuditRequest request) {
        questionService.auditQuestion(id, request);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/status")
    @RequirePermission("question:edit")
    @OperationLog(action = OperationAction.STATUS_CHANGE, targetType = "question", targetName = "题目")
    @Operation(summary = "题目上架/下架")
    public ApiResponse<Void> changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody QuestionStatusRequest request) {
        questionService.changeStatus(id, request.action());
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/resubmit")
    @RequirePermission("question:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "question", targetName = "题目重新提交")
    @Operation(summary = "已驳回题目重新提交")
    public ApiResponse<Void> resubmit(@PathVariable Long id) {
        questionService.resubmit(id);
        return ApiResponse.ok();
    }

    @PostMapping("/batch-status")
    @RequirePermission("question:edit")
    @OperationLog(action = OperationAction.STATUS_CHANGE, targetType = "question", targetName = "批量题目状态")
    @Operation(summary = "批量上架/下架")
    public ApiResponse<QuestionBatchStatusResponse> batchStatus(
            @Valid @RequestBody QuestionBatchStatusRequest request) {
        return ApiResponse.ok(questionService.batchChangeStatus(request.ids(), request.action()));
    }

    @PostMapping("/batch-delete")
    @RequirePermission("question:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "question", targetName = "批量删除题目")
    @Operation(summary = "批量删除题目")
    public ApiResponse<QuestionBatchDeleteResponse> batchDelete(
            @Valid @RequestBody QuestionBatchIdsRequest request) {
        return ApiResponse.ok(questionService.batchDelete(request.ids()));
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("question:import")
    @Operation(summary = "题库 Excel 导入预览")
    public ApiResponse<QuestionImportPreviewResponse> previewImport(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long subjectId) {
        return ApiResponse.ok(questionImportService.preview(file, subjectId));
    }

    @PostMapping("/import/confirm")
    @RequirePermission("question:import")
    @OperationLog(action = OperationAction.CREATE, targetType = "question", targetName = "题库批量导入")
    @Operation(summary = "确认题库 Excel 导入")
    public ApiResponse<QuestionImportConfirmResponse> confirmImport(
            @Valid @RequestBody QuestionImportConfirmRequest request) {
        return ApiResponse.ok(questionImportService.confirm(request.token()));
    }

    @GetMapping("/import/template")
    @RequirePermission("question:import")
    @Operation(summary = "下载题库 Excel 导入模板")
    public ResponseEntity<byte[]> template() {
        byte[] bytes = questionImportService.template();
        return download(bytes, "题库导入模板.xlsx");
    }

    @GetMapping("/export")
    @RequirePermission("question:export")
    @OperationLog(action = OperationAction.EXPORT, targetType = "question", targetName = "题库导出")
    @Operation(summary = "按筛选导出题库 Excel")
    public ResponseEntity<byte[]> export(@Valid QuestionQuery query) {
        byte[] bytes = questionService.exportQuestions(query);
        return download(bytes, "题库导出.xlsx");
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
