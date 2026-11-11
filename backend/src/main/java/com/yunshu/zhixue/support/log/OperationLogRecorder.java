package com.yunshu.zhixue.support.log;

/**
 * 操作日志落库 SPI：由 {@code system} 模块提供实现（异步写入 operation_log）。
 *
 * <p>未提供实现时切面静默跳过，不阻断业务。</p>
 */
public interface OperationLogRecorder {

    void record(OperationLogCommand command);
}
