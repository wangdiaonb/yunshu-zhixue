package com.yunshu.zhixue.common.response;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

/**
 * 分页入参基类：{@code page} 从 1 起（默认 1），{@code pageSize} 默认 10、最大 100。
 *
 * <p>列表接口的查询 DTO 继承本类即可获得统一分页参数与校验。</p>
 */
@Getter
@Setter
public class PageQuery {

    public static final long DEFAULT_PAGE = 1L;
    public static final long DEFAULT_PAGE_SIZE = 10L;
    public static final long MAX_PAGE_SIZE = 100L;

    @Min(value = 1, message = "页码最小为 1")
    private Long page = DEFAULT_PAGE;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数最大为 100")
    private Long pageSize = DEFAULT_PAGE_SIZE;

    /** 归一化后的页码，避免调用方直接使用 null。 */
    public long pageOrDefault() {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    public long pageSizeOrDefault() {
        return pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
