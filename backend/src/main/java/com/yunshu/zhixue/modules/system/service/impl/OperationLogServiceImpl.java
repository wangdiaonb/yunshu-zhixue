package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.common.util.TimeUtils;
import com.yunshu.zhixue.modules.system.dto.OperationLogItem;
import com.yunshu.zhixue.modules.system.dto.OperationLogQuery;
import com.yunshu.zhixue.modules.system.entity.OperationLog;
import com.yunshu.zhixue.modules.system.mapper.OperationLogMapper;
import com.yunshu.zhixue.modules.system.service.OperationLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private static final int EXPORT_BATCH_SIZE = 500;

    private final OperationLogMapper operationLogMapper;

    @Override
    public PageResult<OperationLogItem> pageLogs(OperationLogQuery query) {
        IPage<OperationLog> page = operationLogMapper.selectPage(
                new Page<>(query.pageOrDefault(), query.pageSizeOrDefault()), buildWrapper(query));
        List<OperationLogItem> items = page.getRecords().stream().map(this::toItem).toList();
        return PageResult.of(items, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public OperationLogItem getLog(Long logId) {
        OperationLog log = operationLogMapper.selectById(logId);
        if (log == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "日志不存在");
        }
        return toItem(log);
    }

    @Override
    public void exportLogs(OperationLogQuery query, HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<OperationLog> wrapper = buildWrapper(query);
        response.reset();
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"operation-logs.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\uFEFF');
            writer.println("日志ID,用户,操作,目标类型,目标,IP地址,操作时间");
            long current = 1;
            while (true) {
                Page<OperationLog> page = new Page<>(current, EXPORT_BATCH_SIZE);
                page.setSearchCount(false);
                operationLogMapper.selectPage(page, wrapper);
                List<OperationLog> records = page.getRecords();
                if (records.isEmpty()) {
                    break;
                }
                for (OperationLog log : records) {
                    writer.println(String.join(",",
                            csv(log.getId()),
                            csv(log.getUsername()),
                            csv(log.getAction()),
                            csv(log.getTargetType()),
                            csv(log.getTargetName()),
                            csv(log.getIp()),
                            csv(TimeUtils.formatBiz(log.getCreatedAt()))));
                }
                if (records.size() < EXPORT_BATCH_SIZE) {
                    break;
                }
                current++;
            }
            writer.flush();
        }
    }

    @Override
    public void clearLogs() {
        // 物理清空；为避免留下“清空日志”自身的一条记录，该操作不写操作日志
        operationLogMapper.delete(Wrappers.<OperationLog>lambdaQuery()
                .isNotNull(OperationLog::getId));
    }

    private LambdaQueryWrapper<OperationLog> buildWrapper(OperationLogQuery query) {
        if (query.getStartDate() != null && query.getEndDate() != null
                && query.getEndDate().isBefore(query.getStartDate())) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "结束日期不能早于开始日期");
        }
        LambdaQueryWrapper<OperationLog> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(item -> item.like(OperationLog::getUsername, keyword)
                    .or().like(OperationLog::getAction, keyword)
                    .or().like(OperationLog::getTargetType, keyword)
                    .or().like(OperationLog::getTargetName, keyword));
        }
        if (StringUtils.hasText(query.getOperationType())) {
            wrapper.eq(OperationLog::getAction, query.getOperationType().trim());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(OperationLog::getCreatedAt, TimeUtils.bizDayStartToUtc(query.getStartDate()));
        }
        if (query.getEndDate() != null) {
            wrapper.lt(OperationLog::getCreatedAt, TimeUtils.bizDayEndExclusiveToUtc(query.getEndDate()));
        }
        wrapper.orderByDesc(OperationLog::getCreatedAt).orderByDesc(OperationLog::getId);
        return wrapper;
    }

    private OperationLogItem toItem(OperationLog log) {
        return new OperationLogItem(log.getId(), log.getUserId(), log.getUsername(), log.getAction(),
                log.getTargetType(), log.getTargetName(), log.getIp(),
                TimeUtils.utcToBiz(log.getCreatedAt()));
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
