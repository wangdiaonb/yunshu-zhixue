package com.yunshu.zhixue.modules.system.dto;

import com.yunshu.zhixue.common.response.PageQuery;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 操作日志查询参数（A-G01）。
 *
 * <p>日期范围为 Asia/Shanghai 自然日，服务端转换为 UTC 查询。</p>
 */
@Getter
@Setter
public class OperationLogQuery extends PageQuery {

    /** 用户名 / 操作 / 目标模糊匹配 */
    @Size(max = 50, message = "关键词长度不能超过 50 个字符")
    private String keyword;

    /** 操作类型：登录/登出/新增/修改/删除/禁用启用/导出/其他 */
    @Size(max = 20, message = "操作类型不合法")
    private String operationType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}
