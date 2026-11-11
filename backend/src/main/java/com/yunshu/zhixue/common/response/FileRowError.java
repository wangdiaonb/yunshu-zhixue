package com.yunshu.zhixue.common.response;

/**
 * 批量导入的行级错误（仅业务码 30005 返回）。
 *
 * @param row    Excel/Markdown 源文件行号（从 1 起）
 * @param field  错误的列名或字段名
 * @param reason 错误原因
 */
public record FileRowError(Integer row, String field, String reason) {
}
