package com.yunshu.zhixue.modules.interviewbank.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicSaveRequest;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewTopicService;
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
 * 管理端面试题主题词表接口（A-I10 ~ A-I13）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/interview-topics")
@RequiredArgsConstructor
@Tag(name = "管理端-面试题主题")
public class InterviewTopicAdminController {

    private final InterviewTopicService topicService;

    @GetMapping
    @RequirePermission("interviewbank:view")
    @Operation(summary = "面试题主题列表（含 0 题主题与词表外主题）")
    public ApiResponse<List<InterviewTopicItem>> list(@RequestParam(required = false) Long subjectId) {
        return ApiResponse.ok(topicService.listTopics(subjectId));
    }

    @PostMapping
    @RequirePermission("interviewbank:upload")
    @OperationLog(action = OperationAction.CREATE, targetType = "interview_topic", targetName = "面试题主题")
    @Operation(summary = "新增面试题主题")
    public ApiResponse<Long> create(@Valid @RequestBody InterviewTopicSaveRequest request) {
        return ApiResponse.ok(topicService.createTopic(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("interviewbank:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "interview_topic", targetName = "面试题主题")
    @Operation(summary = "编辑面试题主题")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody InterviewTopicSaveRequest request) {
        topicService.updateTopic(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("interviewbank:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "interview_topic", targetName = "面试题主题")
    @Operation(summary = "删除面试题主题")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        topicService.deleteTopic(id);
        return ApiResponse.ok();
    }
}
