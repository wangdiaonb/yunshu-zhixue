package com.yunshu.zhixue.modules.system.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.OperationLogItem;
import com.yunshu.zhixue.modules.system.dto.OperationLogQuery;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import com.yunshu.zhixue.security.RequirePermission;
import com.yunshu.zhixue.support.log.OperationAction;
import com.yunshu.zhixue.support.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 管理端操作日志接口（A-G01 ~ A-G04）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/logs")
@RequiredArgsConstructor
@Tag(name = "管理端-操作日志")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @RequirePermission("system:log_view")
    @Operation(summary = "操作日志分页列表")
    public ApiResponse<PageResult<OperationLogItem>> page(@Valid OperationLogQuery query) {
        return ApiResponse.ok(operationLogService.pageLogs(query));
    }

    @GetMapping("/{id}")
    @RequirePermission("system:log_view")
    @Operation(summary = "操作日志详情")
    public ApiResponse<OperationLogItem> detail(@PathVariable Long id) {
        return ApiResponse.ok(operationLogService.getLog(id));
    }

    @GetMapping("/export")
    @RequirePermission("system:log_view")
    @OperationLog(action = OperationAction.EXPORT, targetType = "operation_log", targetName = "操作日志")
    @Operation(summary = "导出操作日志 CSV")
    public void export(@Valid OperationLogQuery query, HttpServletResponse response) throws IOException {
        operationLogService.exportLogs(query, response);
    }

    @DeleteMapping
    @RequirePermission("system:log_view")
    @Operation(summary = "清空操作日志（物理删除）")
    public ApiResponse<Void> clear() {
        operationLogService.clearLogs();
        return ApiResponse.ok();
    }
}
