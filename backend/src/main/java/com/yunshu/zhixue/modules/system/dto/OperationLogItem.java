package com.yunshu.zhixue.modules.system.dto;

import java.time.LocalDateTime;

/**
 * 操作日志列表/详情项（时间已转换为 Asia/Shanghai）。
 */
public record OperationLogItem(Long id, Long userId, String username, String action,
                               String targetType, String targetName, String ip,
                               LocalDateTime createdAt) {
}
