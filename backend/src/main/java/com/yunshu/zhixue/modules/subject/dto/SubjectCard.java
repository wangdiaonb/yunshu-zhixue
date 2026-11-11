package com.yunshu.zhixue.modules.subject.dto;

import java.util.List;

/**
 * 学科卡片（A-S02）：管理端包含全部阶段（含禁用），由前端展示启停状态。
 */
public record SubjectCard(Long id, String name, String icon, String description,
                          String themeColor, Integer sortOrder,
                          long courseCount, long routeCount, long questionCount,
                          List<StageItem> stages) {
}
