package com.yunshu.zhixue.modules.system.dto;

import java.util.List;

/**
 * 后台用户详情：基本资料 + 最近操作。
 */
public record AdminUserDetail(AdminUserItem user, List<OperationLogItem> recentLogs) {
}
