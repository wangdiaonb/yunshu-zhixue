package com.yunshu.zhixue.common.response;

import java.util.List;

/**
 * 统一分页响应结构：{@code {list, total, page, pageSize}}。
 *
 * @param list     当前页数据
 * @param total    总条数
 * @param page     页码（从 1 起）
 * @param pageSize 每页条数
 */
public record PageResult<T>(List<T> list, long total, long page, long pageSize) {

    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }

    public static <T> PageResult<T> empty(long page, long pageSize) {
        return new PageResult<>(List.of(), 0L, page, pageSize);
    }
}
