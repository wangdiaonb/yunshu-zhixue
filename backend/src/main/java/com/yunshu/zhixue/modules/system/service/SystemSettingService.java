package com.yunshu.zhixue.modules.system.service;

import com.yunshu.zhixue.modules.system.dto.SystemSettingResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingUpdateRequest;

/**
 * 系统基本信息设置服务（A-T01 ~ A-T03）。
 */
public interface SystemSettingService {

    SystemSettingResponse getSettings();

    SystemSettingResponse updateSettings(SystemSettingUpdateRequest request);

    SystemSettingResponse resetSettings();
}
