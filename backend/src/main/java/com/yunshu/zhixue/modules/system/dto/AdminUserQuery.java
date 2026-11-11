package com.yunshu.zhixue.modules.system.dto;

import com.yunshu.zhixue.common.response.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 后台用户列表查询参数（A-M01）。
 */
@Getter
@Setter
public class AdminUserQuery extends PageQuery {

    /** 用户名或手机号模糊匹配 */
    @Size(max = 50, message = "关键词长度不能超过 50 个字符")
    private String keyword;

    private Long roleId;

    @Min(value = 0, message = "状态不合法")
    @Max(value = 1, message = "状态不合法")
    private Integer status;
}
