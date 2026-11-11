/**
 * 统计模块：首页/个人中心学习数据、面试统计、管理端看板聚合。
 *
 * <p>只读模块：不参与业务写入；热点统计结果入 Redis 短时缓存（5 分钟），
 * 最近活动列表不缓存（T7）。</p>
 */
package com.yunshu.zhixue.modules.stats;
