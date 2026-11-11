package com.yunshu.zhixue.modules.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.modules.system.dto.SystemSettingResponse;
import com.yunshu.zhixue.modules.system.dto.SystemSettingUpdateRequest;
import com.yunshu.zhixue.modules.system.entity.SystemSetting;
import com.yunshu.zhixue.modules.system.mapper.SystemSettingMapper;
import com.yunshu.zhixue.modules.system.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final String KEY_SITE_NAME = "site_name";
    private static final String KEY_SITE_DESCRIPTION = "site_description";
    private static final String KEY_LOGO_URL = "logo_url";
    private static final String KEY_DEFAULT_AVATAR = "default_avatar";

    private static final String DEFAULT_SITE_NAME = "云枢智学";

    private final SystemSettingMapper systemSettingMapper;

    @Override
    public SystemSettingResponse getSettings() {
        Map<String, String> values = loadValues();
        return new SystemSettingResponse(
                values.getOrDefault(KEY_SITE_NAME, DEFAULT_SITE_NAME),
                values.getOrDefault(KEY_SITE_DESCRIPTION, ""),
                values.getOrDefault(KEY_LOGO_URL, ""),
                values.getOrDefault(KEY_DEFAULT_AVATAR, ""));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemSettingResponse updateSettings(SystemSettingUpdateRequest request) {
        String siteName = request.siteName().trim();
        upsert(KEY_SITE_NAME, siteName, "平台名称（必填，显示在浏览器标题与 Logo 旁）");
        upsert(KEY_SITE_DESCRIPTION, normalize(request.siteDescription()), "平台描述（多行文本）");
        upsert(KEY_LOGO_URL, normalize(request.logoUrl()), "Logo 图片地址（建议 256×256）");
        upsert(KEY_DEFAULT_AVATAR, normalize(request.defaultAvatar()), "默认头像地址");
        return getSettings();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemSettingResponse resetSettings() {
        upsert(KEY_SITE_NAME, DEFAULT_SITE_NAME, "平台名称（必填，显示在浏览器标题与 Logo 旁）");
        upsert(KEY_SITE_DESCRIPTION, "", "平台描述（多行文本）");
        upsert(KEY_LOGO_URL, "", "Logo 图片地址（建议 256×256）");
        upsert(KEY_DEFAULT_AVATAR, "", "默认头像地址");
        return getSettings();
    }

    private Map<String, String> loadValues() {
        List<SystemSetting> settings = systemSettingMapper.selectList(Wrappers.<SystemSetting>lambdaQuery()
                .in(SystemSetting::getConfigKey,
                        KEY_SITE_NAME, KEY_SITE_DESCRIPTION, KEY_LOGO_URL, KEY_DEFAULT_AVATAR));
        Map<String, String> values = new HashMap<>();
        for (SystemSetting setting : settings) {
            values.put(setting.getConfigKey(), setting.getConfigValue() == null ? "" : setting.getConfigValue());
        }
        return values;
    }

    private void upsert(String key, String value, String remark) {
        SystemSetting existing = systemSettingMapper.selectOne(Wrappers.<SystemSetting>lambdaQuery()
                .eq(SystemSetting::getConfigKey, key)
                .last("LIMIT 1"));
        if (existing == null) {
            SystemSetting setting = new SystemSetting();
            setting.setConfigKey(key);
            setting.setConfigValue(value);
            setting.setRemark(remark);
            systemSettingMapper.insert(setting);
            return;
        }
        existing.setConfigValue(value);
        existing.setRemark(remark);
        systemSettingMapper.updateById(existing);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
