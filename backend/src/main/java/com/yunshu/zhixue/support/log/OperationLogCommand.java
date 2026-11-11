package com.yunshu.zhixue.support.log;

/**
 * 操作日志记录指令。
 *
 * @param userId     操作人主键
 * @param username   操作人用户名快照
 * @param action     操作类型（中文枚举）
 * @param targetType 操作对象类型
 * @param targetName 操作对象描述
 * @param ip         操作 IP
 */
public record OperationLogCommand(Long userId, String username, String action,
                                  String targetType, String targetName, String ip) {
}
