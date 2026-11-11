package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 确认导入请求（A-C07）。
 */
public record CourseImportConfirmRequest(
        @NotBlank(message = "缺少导入令牌，请重新预览")
        String token
) {
}
