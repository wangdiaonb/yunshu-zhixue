package com.yunshu.zhixue.modules.subject.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 学科/阶段整体排序（A-S05 / A-S09）：按数组顺序写入 sort_order。
 */
public record SubjectSortRequest(

        @NotEmpty(message = "排序列表不能为空")
        @Size(max = 100, message = "排序列表不合法")
        List<Long> orderedIds
) {
}
