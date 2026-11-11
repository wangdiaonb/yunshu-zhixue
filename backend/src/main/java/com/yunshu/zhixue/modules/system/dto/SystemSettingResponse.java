package com.yunshu.zhixue.modules.system.dto;

/**
 * 系统基本信息设置（A-T01）。
 */
public record SystemSettingResponse(String siteName, String siteDescription,
                                    String logoUrl, String defaultAvatar) {
}
