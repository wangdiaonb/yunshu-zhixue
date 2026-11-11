package com.yunshu.zhixue.modules.system.support;

import com.yunshu.zhixue.config.AsyncConfig;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import com.yunshu.zhixue.support.log.OperationLogCommand;
import com.yunshu.zhixue.support.log.OperationLogRecorder;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 操作日志落库实现：异步写入 operation_log，不阻塞业务请求。
 */
@Component
@RequiredArgsConstructor
public class SystemOperationLogRecorder implements OperationLogRecorder {

    private final OperationLogMapper operationLogMapper;

    @Async(AsyncConfig.APP_TASK_EXECUTOR)
    @Override
    public void record(OperationLogCommand command) {
        if (command == null || command.userId() == null) {
            return;
        }
        OperationLog log = new OperationLog();
        log.setUserId(command.userId());
        log.setUsername(command.username());
        log.setAction(command.action());
        log.setTargetType(command.targetType());
        log.setTargetName(command.targetName());
        log.setIp(command.ip());
        log.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        operationLogMapper.insert(log);
    }
}
