package com.yunshu.zhixue.common.constant;

import java.time.Duration;
import java.time.ZoneId;

/**
 * 应用级固定常量（架构 D13：阈值不做后台可配置）。
 *
 * <p>与需求文档 3.2、3.8 保持一致，修改需同步设计文档。</p>
 */
public final class AppConstants {

    private AppConstants() {
    }

    /** 业务时区：存储 UTC，展示与「自然日」判定按 Asia/Shanghai（D17）。 */
    public static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    /** 每日 AI 面试配额（B2）。 */
    public static final int INTERVIEW_DAILY_QUOTA = 5;

    /** AI 面试默认题量。 */
    public static final int INTERVIEW_DEFAULT_QUESTION_COUNT = 10;

    /** AI 面试题量下限与上限。 */
    public static final int INTERVIEW_MIN_QUESTION_COUNT = 3;
    public static final int INTERVIEW_MAX_QUESTION_COUNT = 15;

    /** 单题作答超时（需求 3.8.2：5 分钟）。 */
    public static final Duration INTERVIEW_ANSWER_TIMEOUT = Duration.ofMinutes(5);

    /** 简答 AI 批改通过分（≥6 分记为通过）。 */
    public static final int SHORT_ANSWER_PASS_SCORE = 6;

    /** 错题连续答对达到该次数即标记已掌握。 */
    public static final int WRONG_QUESTION_MASTERED_STREAK = 3;

    /** 题库导入单次条数上限。 */
    public static final int IMPORT_MAX_ROWS = 500;

    /** 短信验证码有效期与发送冷却、每日上限（D18）。 */
    public static final Duration SMS_CODE_TTL = Duration.ofMinutes(5);
    public static final Duration SMS_SEND_COOLDOWN = Duration.ofSeconds(60);
    public static final int SMS_DAILY_LIMIT = 10;

    /** 登录失败锁定策略（D18）。 */
    public static final int LOGIN_MAX_FAILURES = 5;
    public static final Duration LOGIN_LOCK_DURATION = Duration.ofMinutes(5);

    /** SSE 心跳间隔（API 设计 §2.9）。 */
    public static final Duration SSE_HEARTBEAT_INTERVAL = Duration.ofSeconds(15);

    /** 面试会话题目快照在 Redis 的保留时长（D4-4）。 */
    public static final Duration INTERVIEW_TEMP_TTL = Duration.ofHours(24);

    /** 看板热点统计缓存时长（T7）。 */
    public static final Duration DASHBOARD_CACHE_TTL = Duration.ofMinutes(5);

    /** 请求头与令牌前缀。 */
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String REFRESH_TOKEN_HEADER = "X-Refresh-Token";
    public static final String BEARER_PREFIX = "Bearer ";

    /** 接口路径前缀（D20）。 */
    public static final String PORTAL_API_PREFIX = "/api/portal/v1";
    public static final String ADMIN_API_PREFIX = "/api/admin/v1";
}
