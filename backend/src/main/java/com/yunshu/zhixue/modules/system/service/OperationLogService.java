package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.system.dto.OperationLogItem;
import com.yunshu.zhixue.modules.system.dto.OperationLogQuery;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 操作日志服务（A-G01 ~ A-G04）。
 */
public interface OperationLogService {

    PageResult<OperationLogItem> pageLogs(OperationLogQuery query);

    OperationLogItem getLog(Long logId);

    /** 导出当前筛选条件下的全部日志为 CSV（带 BOM）。 */
    void exportLogs(OperationLogQuery query, HttpServletResponse response) throws IOException;

    /** 清空全部日志（物理删除）。 */
    void clearLogs();
}
