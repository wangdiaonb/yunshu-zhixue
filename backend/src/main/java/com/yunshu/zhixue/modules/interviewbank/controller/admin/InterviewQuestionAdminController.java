package com.yunshu.zhixue.modules.interviewbank.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionCreateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionDetail;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportConfirmRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportConfirmResponse;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportPreviewResponse;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionQuery;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionStatusRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionUpdateRequest;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewQuestionImportService;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewQuestionService;
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
 * 管理端面试题库接口（A-I01 ~ A-I08、A-I15 ~ A-I17）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/interview-questions")
@RequiredArgsConstructor
@Tag(name = "管理端-面试题库管理")
public class InterviewQuestionAdminController {

    private final InterviewQuestionService questionService;
    private final InterviewQuestionImportService importService;

    @GetMapping
    @RequirePermission("interviewbank:view")
    @Operation(summary = "面试题分页列表")
    public ApiResponse<PageResult<InterviewQuestionItem>> page(@Valid InterviewQuestionQuery query) {
        return ApiResponse.ok(questionService.pageQuestions(query));
    }

    @GetMapping("/import/template")
    @RequirePermission("interviewbank:upload")
    @Operation(summary = "下载面试题 Excel 导入模板")
    public ResponseEntity<byte[]> template() {
        return download(importService.template(), "面试题导入模板.xlsx");
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("interviewbank:upload")
    @Operation(summary = "面试题 Excel 导入预览")
    public ApiResponse<InterviewQuestionImportPreviewResponse> previewImport(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) Long defaultSubjectId) {
        return ApiResponse.ok(importService.preview(file, defaultSubjectId));
    }

    @PostMapping("/import/confirm")
    @RequirePermission("interviewbank:upload")
    @OperationLog(action = OperationAction.CREATE, targetType = "interview_question", targetName = "面试题批量导入")
    @Operation(summary = "确认面试题 Excel 导入")
    public ApiResponse<InterviewQuestionImportConfirmResponse> confirmImport(
            @Valid @RequestBody InterviewQuestionImportConfirmRequest request) {
        return ApiResponse.ok(importService.confirm(request.token()));
    }

    @GetMapping("/{id}")
    @RequirePermission("interviewbank:view")
    @Operation(summary = "面试题详情")
    public ApiResponse<InterviewQuestionDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(questionService.getQuestion(id));
    }

    @PostMapping
    @RequirePermission("interviewbank:upload")
    @OperationLog(action = OperationAction.CREATE, targetType = "interview_question", targetName = "面试题")
    @Operation(summary = "手工录入面试题")
    public ApiResponse<Long> create(@Valid @RequestBody InterviewQuestionCreateRequest request) {
        return ApiResponse.ok(questionService.createQuestion(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("interviewbank:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "interview_question", targetName = "面试题主题难度")
    @Operation(summary = "编辑面试题（仅主题与难度）")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody InterviewQuestionUpdateRequest request) {
        questionService.updateQuestion(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("interviewbank:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "interview_question", targetName = "面试题")
    @Operation(summary = "删除面试题")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/status")
    @RequirePermission("interviewbank:edit")
    @OperationLog(action = OperationAction.STATUS_CHANGE, targetType = "interview_question", targetName = "面试题")
    @Operation(summary = "面试题上架/下架")
    public ApiResponse<Void> changeStatus(@PathVariable Long id,
                                          @Valid @RequestBody InterviewQuestionStatusRequest request) {
        questionService.changeStatus(id, request.status());
        return ApiResponse.ok();
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
