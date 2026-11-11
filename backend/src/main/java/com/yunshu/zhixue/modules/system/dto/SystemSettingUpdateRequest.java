package com.yunshu.zhixue.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 保存系统基本信息设置（A-T02）。
 */
public record SystemSettingUpdateRequest(

        @NotBlank(message = "请输入平台名称")
        @Size(max = 50, message = "平台名称不能超过 50 个字符")
        String siteName,

        @Size(max = 500, message = "平台描述不能超过 500 个字符")
        String siteDescription,

        @Size(max = 500, message = "Logo 地址不能超过 500 个字符")
        String logoUrl,

        @Size(max = 500, message = "默认头像地址不能超过 500 个字符")
        String defaultAvatar
) {
}
