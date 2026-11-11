package com.yunshu.zhixue.modules.student.dto;

import com.yunshu.zhixue.common.response.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 学员列表筛选（A-U01）。
 */
@Getter
@Setter
public class StudentQuery extends PageQuery {

    /** 昵称 / 手机号模糊搜索。 */
    @Size(max = 50, message = "搜索关键词不能超过 50 个字符")
    private String keyword;

    /** 1 已启用 / 0 已禁用；为空表示全部。 */
    @Min(value = 0, message = "状态不合法")
    @Max(value = 1, message = "状态不合法")
    private Integer status;
}
