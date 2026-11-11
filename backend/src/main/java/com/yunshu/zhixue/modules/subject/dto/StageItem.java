package com.yunshu.zhixue.modules.subject.dto;

/**
 * 学科卡片中的阶段项。
 */
public record StageItem(Long id, String name, String icon, Integer sortOrder,
                        Integer status, long questionCount) {
}
