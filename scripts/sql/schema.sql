-- =============================================================================
-- 云枢智学 · 数据库建表脚本（唯一建表脚本）
-- -----------------------------------------------------------------------------
-- 依据文档：docs/design/03.数据库设计.md（24 张业务表）
-- 设计规范：docs/rules/database-rules.md
-- 关联文档：docs/design/01.功能模块梳理.md、02.整体架构设计.md、04.API接口设计.md、
--           docs/云枢智学-需求文档.md、prototype/
--           （学科种子取自原型 MockData.subjects；权限编码与 id 以 04 附录 A（D4-5 定稿）为准）
--
-- 环境约定：
--   1. MySQL 8.0+，数据库 yszx，字符集 utf8mb4 / 排序规则 utf8mb4_0900_ai_ci。
--   2. 所有 DATETIME 统一按 UTC 存储，展示层转 Asia/Shanghai（D17）。
--      本脚本将当前会话时区设为 +00:00，使 CURRENT_TIMESTAMP 默认值落 UTC；
--      应用连接串/连接池也必须设置同样的时区，否则默认值会产生时区偏移。
--   3. 全部禁用物理 FOREIGN KEY，表间仅用 <实体>_id 逻辑关联，
--      关联校验与删除保护由应用层（service 事务内）保证。
--   4. 业务表统一逻辑删除 delete_flag（0 未删除 / 1 已删除），
--      由 MyBatis-Plus 逻辑删除插件在查询侧过滤。
--      例外（D3-3 / §6）：operation_log、role_permission 无 delete_flag，
--      「清空日志」「编辑权限保存」均为物理删除。
--   5. 本脚本可重复执行：CREATE TABLE IF NOT EXISTS + INSERT IGNORE（幂等种子）。
--      实施阶段的增量迁移另行拆分，不在本文件内维护。
--
-- ⚠️ 部署前必做：内置超级管理员 admin 的 password_hash 为占位符，
--    必须在部署初始化时重置为默认密码并生成有效 BCrypt 哈希，切勿用占位值登录。
--
-- 文档内部歧义备案（不阻塞建表，供审核时确认）：
--   03 文档 §4.5 system_setting 表结构明细只列出 created_at / updated_at，
--   未列 delete_flag；但 §2「全站业务表 delete_flag」与 §6「例外仅 2 张表」
--   均要求业务表统一逻辑删除。本脚本按全局规则为 system_setting 补上 delete_flag。
-- =============================================================================

SET NAMES utf8mb4;
SET SESSION time_zone = '+00:00';

CREATE DATABASE IF NOT EXISTS `yszx`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `yszx`;


-- =============================================================================
-- 一、学员端账号（auth / student）
-- =============================================================================

-- 1. student 学员表
CREATE TABLE IF NOT EXISTS `student` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `mobile`               VARCHAR(11)  NOT NULL                COMMENT '手机号（11 位，登录账号）',
  `password_hash`        VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密码哈希',
  `nickname`             VARCHAR(50)  NOT NULL                COMMENT '用户名（个人中心可编辑，注册时应用层赋默认值）',
  `avatar`               VARCHAR(500) DEFAULT NULL            COMMENT '头像 URL（MinIO）',
  `email`                VARCHAR(100) DEFAULT NULL            COMMENT '邮箱（可留空；唯一索引允许多行 NULL，非空值唯一）',
  `gender`               TINYINT      NOT NULL DEFAULT 0      COMMENT '性别：0 保密 / 1 男 / 2 女',
  `preferred_subject_id` BIGINT UNSIGNED DEFAULT NULL         COMMENT '技术方向 → subject.id（D3-4）',
  `guide_finished`       TINYINT      NOT NULL DEFAULT 0      COMMENT '新手引导：0 未看过 / 1 已看过（D3-4）',
  `status`               TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 已启用 / 0 已禁用',
  `last_login_at`        DATETIME     DEFAULT NULL            COMMENT '最后登录时间（UTC）',
  `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`          TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_mobile` (`mobile`),
  UNIQUE KEY `uk_student_email` (`email`),
  KEY `idx_student_created_at` (`created_at`),
  KEY `idx_student_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员表';

-- 1.1 student_oauth 学员第三方账号绑定表（R-03 新增：微信 / QQ / GitHub）
--     第三方 access_token / refresh_token 不落库；授权 state 与待绑定 ticket 存 Redis（TTL 10min）。
CREATE TABLE IF NOT EXISTS `student_oauth` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`   BIGINT UNSIGNED NOT NULL             COMMENT '学员 → student.id',
  `provider`     TINYINT      NOT NULL                COMMENT '第三方平台：1 微信 / 2 QQ / 3 GitHub',
  `open_id`      VARCHAR(64)  NOT NULL                COMMENT '平台内用户唯一标识（微信 openid / QQ openid / GitHub user id）',
  `union_id`     VARCHAR(64)  DEFAULT NULL            COMMENT '微信开放平台 unionid（多应用互通；其他平台为空）',
  `nickname`     VARCHAR(50)  DEFAULT NULL            COMMENT '第三方昵称快照（仅展示，不覆盖学员昵称）',
  `avatar`       VARCHAR(500) DEFAULT NULL            COMMENT '第三方头像 URL 快照（外链可能失效，仅绑定留痕）',
  `bound_at`     DATETIME     NOT NULL                COMMENT '绑定时间（UTC）',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_oauth_openid` (`provider`, `open_id`),
  UNIQUE KEY `uk_student_oauth_student` (`student_id`, `provider`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员第三方账号绑定表（微信/QQ/GitHub）';


-- =============================================================================
-- 二、内容管理（subject / course / question / interviewbank / route）
-- =============================================================================

-- 2. subject 学科表
CREATE TABLE IF NOT EXISTS `subject` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(50)  NOT NULL                COMMENT '学科名称（8 个 AI 学科种子，可新增）',
  `icon`         VARCHAR(50)  NOT NULL                COMMENT '学科图标（Emoji，必填）',
  `description`  VARCHAR(255) NOT NULL                COMMENT '学科描述',
  `theme_color`  VARCHAR(20)  NOT NULL DEFAULT '#000000' COMMENT '主题色（预置色板）',
  `sort_order`   INT          NOT NULL DEFAULT 0      COMMENT '排序（越小越靠前，支持上移/下移）',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_name` (`name`),
  KEY `idx_subject_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学科表';

-- 3. stage 阶段表
CREATE TABLE IF NOT EXISTS `stage` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id`   BIGINT UNSIGNED NOT NULL             COMMENT '所属学科 → subject.id',
  `name`         VARCHAR(50)  NOT NULL                COMMENT '阶段名称',
  `icon`         VARCHAR(50)  DEFAULT NULL            COMMENT '阶段图标（留空走默认）',
  `sort_order`   INT          NOT NULL DEFAULT 0      COMMENT '排序（越小越靠前）',
  `status`       TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 启用 / 0 禁用',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_subject_name` (`subject_id`, `name`),
  KEY `idx_stage_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阶段表（学科 1:n）';

-- 4. course B 站课程库
CREATE TABLE IF NOT EXISTS `course` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`              VARCHAR(100) NOT NULL                COMMENT '课程名称',
  `cover_url`         VARCHAR(500) DEFAULT NULL            COMMENT '封面 URL（外链）',
  `duration_minutes`  INT          NOT NULL DEFAULT 0      COMMENT '时长（分钟），展示时转 小时:分钟',
  `source`            TINYINT      NOT NULL DEFAULT 1      COMMENT '来源：1 B站 / 2 其他',
  `link`              VARCHAR(500) DEFAULT NULL            COMMENT '课程链接',
  `bvid`              VARCHAR(20)  DEFAULT NULL            COMMENT 'BV 号（非 B 站来源为空；唯一索引允许多行 NULL）',
  `subject_id`        BIGINT UNSIGNED DEFAULT NULL         COMMENT '主学科 → subject.id；NULL=「其他学科」（D3-1）',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_bvid` (`bvid`),
  KEY `idx_course_subject` (`subject_id`),
  KEY `idx_course_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='B 站课程库';

-- 5. course_subject 课程-学科多学科关联表（主学科在 course.subject_id，本表存额外学科）
CREATE TABLE IF NOT EXISTS `course_subject` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `course_id`    BIGINT UNSIGNED NOT NULL             COMMENT '课程 → course.id',
  `subject_id`   BIGINT UNSIGNED NOT NULL             COMMENT '额外学科 → subject.id',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_course_subject` (`course_id`, `subject_id`),
  KEY `idx_course_subject_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程-学科关联表（额外学科，D3-1）';

-- 6. question 题库题目表
CREATE TABLE IF NOT EXISTS `question` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `stage_id`      BIGINT UNSIGNED NOT NULL             COMMENT '所属阶段 → stage.id',
  `type`          TINYINT      NOT NULL                COMMENT '题型：1 单选 / 2 多选 / 3 判断 / 4 简答',
  `difficulty`    TINYINT      NOT NULL DEFAULT 2      COMMENT '难度：1 简单 / 2 中等 / 3 困难',
  `content`       TEXT         NOT NULL                COMMENT '题干',
  `answer`        TEXT         DEFAULT NULL            COMMENT '判断题「正确/错误」文本、简答参考答案；单选/多选为 NULL（正确项在 question_option）',
  `analysis`      TEXT         DEFAULT NULL            COMMENT '解析',
  `tags`          VARCHAR(255) DEFAULT NULL            COMMENT '标签（逗号分隔，仅展示，无筛选/统计需求）',
  `status`        TINYINT      NOT NULL DEFAULT 3      COMMENT '状态：1 已上架 / 2 已下架 / 3 待审核 / 4 草稿(预留不启用) / 5 已驳回',
  `reject_reason` VARCHAR(255) DEFAULT NULL            COMMENT '驳回原因（审核不通过时写入；通过或重新提交审核时清空，D4-9）',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`   TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_question_stage` (`stage_id`),
  KEY `idx_question_status` (`status`),
  KEY `idx_question_type_difficulty` (`type`, `difficulty`),
  KEY `idx_question_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题库题目表';

-- 7. question_option 题目选项表（判断/简答题无选项行）
CREATE TABLE IF NOT EXISTS `question_option` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `question_id`  BIGINT UNSIGNED NOT NULL             COMMENT '题目 → question.id',
  `option_key`   VARCHAR(4)   NOT NULL                COMMENT '选项键：A / B / C / D',
  `option_text`  VARCHAR(500) NOT NULL                COMMENT '选项内容',
  `is_correct`   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否正确：1 正确 / 0 否（至少 1 个正确项由应用层校验）',
  `sort_order`   INT          NOT NULL DEFAULT 0      COMMENT '选项排序',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_option` (`question_id`, `option_key`),
  KEY `idx_question_option_qid` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目选项表';

-- 8. interview_question 面试题库表
CREATE TABLE IF NOT EXISTS `interview_question` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id`        BIGINT UNSIGNED NOT NULL             COMMENT '方向 → subject.id（A2/A3：方向=学科）',
  `content`           TEXT         NOT NULL                COMMENT '题干',
  `reference_answer`  TEXT         DEFAULT NULL            COMMENT '参考答案（可留空）',
  `topic`             VARCHAR(50)  DEFAULT NULL            COMMENT '主题（NULL=未分类；词表外主题可录入，筛选时标注）',
  `difficulty`        TINYINT      NOT NULL DEFAULT 2      COMMENT '难度：1 简单 / 2 中等 / 3 困难',
  `tags`              VARCHAR(255) DEFAULT NULL            COMMENT '标签（逗号分隔）',
  `source_type`       TINYINT      NOT NULL DEFAULT 1      COMMENT '来源：1 知识库文档解析 / 2 手工录入 / 3 手动导入',
  `source_doc_name`   VARCHAR(255) DEFAULT NULL            COMMENT '来源文档名（文件不入库，仅留名）',
  `source_seq`        INT          DEFAULT NULL            COMMENT '文档内第 N 题',
  `status`            TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 已上架 / 2 已下架（D3-2，AI 面试出题仅取已上架）',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_interview_question_subject_status` (`subject_id`, `status`),
  KEY `idx_interview_question_topic` (`topic`),
  KEY `idx_interview_question_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题库表';

-- 9. interview_topic 面试题主题词表（初始为空，由运营维护 / 录入导入时提示）
CREATE TABLE IF NOT EXISTS `interview_topic` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subject_id`   BIGINT UNSIGNED DEFAULT NULL          COMMENT '所属学科 → subject.id；NULL=全局主题',
  `name`         VARCHAR(50)  NOT NULL                COMMENT '主题名称',
  `sort_order`   INT          NOT NULL DEFAULT 0      COMMENT '排序',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_interview_topic_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试题主题词表';

-- 10. learning_route 学习路线表（单学科）
CREATE TABLE IF NOT EXISTS `learning_route` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`              VARCHAR(100) NOT NULL                COMMENT '路线名称',
  `subject_id`        BIGINT UNSIGNED NOT NULL             COMMENT '关联学科 → subject.id（单学科）',
  `status`            TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 启用 / 0 禁用',
  `icon`              VARCHAR(50)  DEFAULT NULL            COMMENT '路线图标',
  `target_education`  VARCHAR(100) DEFAULT NULL            COMMENT '适合学历',
  `description`       TEXT         DEFAULT NULL            COMMENT '路线描述',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_learning_route_subject` (`subject_id`),
  KEY `idx_learning_route_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习路线表';

-- 11. route_course 路线-课程关联表（sort_order 即学习顺序，含跨学科补充课程）
CREATE TABLE IF NOT EXISTS `route_course` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `route_id`     BIGINT UNSIGNED NOT NULL             COMMENT '路线 → learning_route.id',
  `course_id`    BIGINT UNSIGNED NOT NULL             COMMENT '课程 → course.id（含跨学科补充课程）',
  `sort_order`   INT          NOT NULL DEFAULT 0      COMMENT '课程顺序（顺序即学习顺序）',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_course` (`route_id`, `course_id`),
  KEY `idx_route_course_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='路线-课程关联表';

-- 12. route_course_completion 学员课程完成表（支撑「已学课程」学习统计）
CREATE TABLE IF NOT EXISTS `route_course_completion` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`       BIGINT UNSIGNED NOT NULL             COMMENT '学员 → student.id',
  `route_course_id`  BIGINT UNSIGNED NOT NULL             COMMENT '路线课程 → route_course.id',
  `completed_at`     DATETIME     NOT NULL                COMMENT '完成时间（UTC）',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_route_course_completion` (`student_id`, `route_course_id`),
  KEY `idx_route_course_completion_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学员课程完成表';


-- =============================================================================
-- 三、学习训练（study）
-- =============================================================================

-- 13. practice_record 刷题作答明细表（学习统计 / 题目答题统计 / 错题本的事实来源）
--     仅已完成作答落库，未完成练习进度仅前端本地；question 不冗余存答题次数。
CREATE TABLE IF NOT EXISTS `practice_record` (
  `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`        BIGINT UNSIGNED NOT NULL             COMMENT '学员 → student.id',
  `question_id`       BIGINT UNSIGNED NOT NULL             COMMENT '题目 → question.id',
  `mode`              TINYINT      NOT NULL DEFAULT 1      COMMENT '练习模式：1 顺序 / 2 随机',
  `is_correct`        TINYINT      NOT NULL DEFAULT 0      COMMENT '是否答对：1 对 / 0 错',
  `duration_seconds`  INT          DEFAULT NULL            COMMENT '该题已用时长（秒）',
  `practiced_at`      DATETIME     NOT NULL                COMMENT '作答时间（UTC）',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`       TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_practice_record_student_time` (`student_id`, `practiced_at`),
  KEY `idx_practice_record_student_question` (`student_id`, `question_id`),
  KEY `idx_practice_record_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='刷题作答明细表';

-- 14. wrong_question 错题本表（每人每题一行）
--     错误等级由 error_count 推导（≥3 高频 / =2 多次 / <2 偶尔），不落等级字段；
--     「移除」=逻辑删除，再次答错时恢复 delete_flag=0 复用本行，避免唯一键冲突。
CREATE TABLE IF NOT EXISTS `wrong_question` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`       BIGINT UNSIGNED NOT NULL             COMMENT '学员 → student.id',
  `question_id`      BIGINT UNSIGNED NOT NULL             COMMENT '题目 → question.id',
  `error_count`      INT          NOT NULL DEFAULT 1      COMMENT '错误次数（等级由查询推导）',
  `corrected_count`  INT          NOT NULL DEFAULT 0      COMMENT '已纠正次数（连续答对 +1，达 3 自动置已掌握）',
  `status`           TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 待复习 / 2 已掌握',
  `last_wrong_at`    DATETIME     DEFAULT NULL            COMMENT '最近答错时间（UTC，列表倒序）',
  `last_answer`      TEXT         DEFAULT NULL            COMMENT '最近一次错误作答快照（D4-8，支撑错题浏览「你的答案」）',
  `version`          INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号（MyBatis-Plus @Version，计数并发更新冲突返回 10004）',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wrong_question` (`student_id`, `question_id`),
  KEY `idx_wrong_question_student_status` (`student_id`, `status`),
  KEY `idx_wrong_question_last_wrong` (`last_wrong_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='错题本表';


-- =============================================================================
-- 四、AI 面试（interview）
-- =============================================================================

-- 15. interview_record 面试记录表（仅已完成会话落库，D16）
CREATE TABLE IF NOT EXISTS `interview_record` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_id`      BIGINT UNSIGNED NOT NULL             COMMENT '学员 → student.id',
  `subject_id`      BIGINT UNSIGNED NOT NULL             COMMENT '面试方向 → subject.id',
  `difficulty`      TINYINT      NOT NULL                COMMENT '难度：1 简单 / 2 中等 / 3 困难',
  `question_count`  INT          NOT NULL                COMMENT '题数（3 / 5 / 8 / 10 / 15）',
  `overall_score`   DECIMAL(3,1) DEFAULT NULL            COMMENT '综合评分（平均分 0.0-10.0）',
  `total_duration`  INT          DEFAULT NULL            COMMENT '累计时长（秒）',
  `started_at`      DATETIME     NOT NULL                COMMENT '开始时间（UTC）',
  `completed_at`    DATETIME     NOT NULL                COMMENT '完成时间（UTC）',
  `version`         INT          NOT NULL DEFAULT 0      COMMENT '乐观锁版本号（MyBatis-Plus @Version，结束面试落库并发保护，冲突返回 10004）',
  `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_interview_record_student_time` (`student_id`, `created_at`),
  KEY `idx_interview_record_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试记录表';

-- 16. interview_question_score 面试逐题评分表（题干快照支撑回放，不受原题修改影响）
CREATE TABLE IF NOT EXISTS `interview_question_score` (
  `id`                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interview_record_id`  BIGINT UNSIGNED NOT NULL             COMMENT '面试记录 → interview_record.id',
  `question_order`       INT          NOT NULL                COMMENT '第 N 题（从 1 起）',
  `question_id`          BIGINT UNSIGNED DEFAULT NULL         COMMENT '原题 → interview_question.id（可空=快照题）',
  `question_text`        TEXT         NOT NULL                COMMENT '题干快照（回放展示）',
  `student_answer`       TEXT         DEFAULT NULL            COMMENT '学员作答',
  `score`                DECIMAL(3,1) NOT NULL                COMMENT '本题得分 0.0-10.0',
  `ai_feedback`          TEXT         DEFAULT NULL            COMMENT 'AI 评分理由 / 反馈',
  `created_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`          TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  KEY `idx_interview_score_record` (`interview_record_id`, `question_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试逐题评分表';

-- 17. interview_report 面试报告表（1:1）
CREATE TABLE IF NOT EXISTS `interview_report` (
  `id`                       BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `interview_record_id`      BIGINT UNSIGNED NOT NULL             COMMENT '面试记录 → interview_record.id（1:1 唯一）',
  `radar_json`               TEXT         DEFAULT NULL            COMMENT '6 维能力分析（JSON）',
  `strengths`                TEXT         DEFAULT NULL            COMMENT '优势 / 表现亮点',
  `weaknesses`               TEXT         DEFAULT NULL            COMMENT '不足 / 薄弱点',
  `suggestions`              TEXT         DEFAULT NULL            COMMENT '改进建议',
  `improvement_cards_json`   TEXT         DEFAULT NULL            COMMENT '3 张能力提升卡（推荐题目 / 课程 / 再次面试，JSON）',
  `created_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`              TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interview_report_record` (`interview_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='面试报告表';


-- =============================================================================
-- 五、系统管理（system）
-- =============================================================================

-- 18. admin_user 后台用户表（单角色；内置超管 is_builtin=1 不可删除/不可改角色）
CREATE TABLE IF NOT EXISTS `admin_user` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`       VARCHAR(50)  NOT NULL                COMMENT '用户名（登录账号）',
  `password_hash`  VARCHAR(100) NOT NULL                COMMENT 'BCrypt 密码哈希',
  `mobile`         VARCHAR(11)  DEFAULT NULL            COMMENT '手机号（常规用户必填由应用层校验；内置超管部署时配置）',
  `email`          VARCHAR(100) DEFAULT NULL            COMMENT '邮箱（选填）',
  `role_id`        BIGINT UNSIGNED NOT NULL             COMMENT '角色 → role.id（单角色，下拉单选）',
  `status`         TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1 已启用 / 0 已禁用',
  `is_builtin`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否内置：1 是（超级管理员，不可删除/改角色） / 0 否',
  `last_login_at`  DATETIME     DEFAULT NULL            COMMENT '最后登录时间（UTC）',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`    TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_username` (`username`),
  UNIQUE KEY `uk_admin_user_mobile` (`mobile`),
  KEY `idx_admin_user_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='后台用户表';

-- 19. role 角色表（预置 3 角色；角色下有用户时应用层禁用删除）
CREATE TABLE IF NOT EXISTS `role` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(50)  NOT NULL                COMMENT '角色名称',
  `code`         VARCHAR(50)  NOT NULL                COMMENT '角色编码（唯一）',
  `description`  VARCHAR(255) DEFAULT NULL            COMMENT '角色描述',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表';

-- 20. permission 权限表（两层级权限树：组节点 type=1 / 操作点 type=2）
--     种子 9 组 + 34 项操作点（D3-5 / D4-5），编码取自需求 §4.5.2 与原型 roles.html。
CREATE TABLE IF NOT EXISTS `permission` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id`    BIGINT UNSIGNED DEFAULT NULL          COMMENT '父节点 → permission.id（NULL=组节点）',
  `name`         VARCHAR(50)  NOT NULL                COMMENT '组名 / 权限点名',
  `code`         VARCHAR(100) NOT NULL                COMMENT '权限编码（如 question:view；组节点形如 group:question）',
  `type`         TINYINT      NOT NULL                COMMENT '类型：1 组（菜单级） / 2 操作点',
  `sort_order`   INT          NOT NULL DEFAULT 0      COMMENT '排序',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`  TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`),
  KEY `idx_permission_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表（权限树 9 组 34 项）';

-- 21. role_permission 角色-权限关联表
--     §6 例外：无 delete_flag、无 updated_at；编辑权限保存=物理删旧+插新（全量覆盖）。
CREATE TABLE IF NOT EXISTS `role_permission` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`        BIGINT UNSIGNED NOT NULL             COMMENT '角色 → role.id',
  `permission_id`  BIGINT UNSIGNED NOT NULL             COMMENT '权限 → permission.id',
  `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-权限关联表（无逻辑删除）';

-- 22. operation_log 操作日志表（AOP 切面异步落库）
--     D3-3 例外：无 delete_flag，审计日志不做逻辑删除，「清空日志」=物理删除。
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键 / 日志 ID',
  `user_id`      BIGINT UNSIGNED NOT NULL             COMMENT '操作用户 → admin_user.id',
  `username`     VARCHAR(50)  NOT NULL                COMMENT '用户名快照（可追溯）',
  `action`       VARCHAR(50)  NOT NULL                COMMENT '操作类型：登录/登出/新增/修改/删除/禁用启用/导出/其他',
  `target_type`  VARCHAR(50)  NOT NULL                COMMENT '操作对象类型（question / course / student 等）',
  `target_name`  VARCHAR(255) NOT NULL                COMMENT '操作对象描述',
  `ip`           VARCHAR(64)  NOT NULL                COMMENT 'IP 地址',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间（UTC）',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user` (`user_id`),
  KEY `idx_operation_log_time` (`created_at`),
  KEY `idx_operation_log_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表（审计，无逻辑删除）';

-- 23. system_setting 系统设置表（key-value，需求 4.5.4 基本信息设置 4 项）
--     delete_flag 依据 03 文档 §2 / §6 全局逻辑删除规则补齐（见文件头「歧义备案」）。
CREATE TABLE IF NOT EXISTS `system_setting` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`    VARCHAR(50)  NOT NULL                COMMENT '配置键（唯一）',
  `config_value`  TEXT         DEFAULT NULL            COMMENT '配置值（文本 / JSON）',
  `remark`        VARCHAR(255) DEFAULT NULL            COMMENT '说明',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（UTC）',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（UTC）',
  `delete_flag`   TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0 未删除 / 1 已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_setting_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统设置表（key-value）';


-- =============================================================================
-- 六、种子数据（03 文档 §7）
--     INSERT IGNORE 保证重复执行幂等；显式 id 便于权限树 parent_id 与角色关联引用。
-- =============================================================================

-- 6.1 学科：8 个 AI 学科（需求 3.8.1，AI应用与智能体开发默认第一）
--     图标 / 描述 / 主题色 / 排序取自 prototype/js/common.js MockData.subjects 占位值，
--     正式上线前由运营在管理端调整（03 文档 §9 第 4 条）。
INSERT IGNORE INTO `subject` (`id`, `name`, `icon`, `description`, `theme_color`, `sort_order`) VALUES
  (1, 'AI应用与智能体开发',   '🤖', 'AI应用开发、RAG检索增强、Agent智能体、MCP协议、后端工程', '#667eea', 1),
  (2, 'AI大模型开发',         '🧠', '大模型微调与部署、机器学习、深度学习、NLP/CV',           '#ff758c', 2),
  (3, 'AI具身智能机器人开发', '🦾', '具身智能、机器人控制、传感器融合、仿真环境',             '#fa709a', 3),
  (4, 'AI嵌入式_机器人开发',  '🔩', '嵌入式Linux、ROS机器人、边缘AI部署、ARM架构',            '#43e97b', 4),
  (5, 'AI测试',               '🧪', 'AI驱动测试、自动化测试、性能与安全测试、智能断言',       '#20c997', 5),
  (6, 'AI运维',               '🔧', '云原生、容器化、CI/CD、AIops智能运维、监控告警',         '#4facfe', 6),
  (7, 'AI视觉设计',           '🎨', 'AI辅助UI设计、AIGC视觉创作、前端工程化',                 '#a18cd1', 7),
  (8, 'AI视频与短剧创作',     '🎬', 'AI视频生成、短剧制作、AIGC内容创作、数字人',             '#f093fb', 8);

-- 6.2 预置 3 角色（需求 4.5.2）
INSERT IGNORE INTO `role` (`id`, `name`, `code`, `description`) VALUES
  (1, '超级管理员', 'super_admin',      '拥有全部权限，含系统管理（系统内置唯一账号所属角色）'),
  (2, '内容审核',   'content_reviewer', '只把关不生产：查看全部内容并审核题目，无增删改权限'),
  (3, '运营人员',   'operator',         '除系统管理外的全部操作，含内容维护与学员管理，不具备审核权限');

-- 6.3 权限树：9 组 + 34 项操作点（D4-5 定稿）
--     编码与 id 与 docs/design/04.API接口设计.md 附录 A 一一对应：
--     组节点 id 1~9（type=1，仅用于权限树展示，不参与授权）；
--     操作点 id 按组分段（十位数 = 组 id）：
--       11~17 题库管理 / 21~25 面试题库 / 31~34 用户管理 / 41~44 学习路线 /
--       51~53 学员管理 / 61~62 系统管理 / 71~74 学科管理 / 75~78 课程库 / 79 数据看板。
INSERT IGNORE INTO `permission` (`id`, `parent_id`, `name`, `code`, `type`, `sort_order`) VALUES
  -- 组节点（type=1，parent_id=NULL）
  (1, NULL, '题库管理',     'group:question',      1, 1),
  (2, NULL, '面试题库管理', 'group:interviewbank', 1, 2),
  (3, NULL, '用户管理',     'group:user',          1, 3),
  (4, NULL, '学习路线',     'group:route',         1, 4),
  (5, NULL, '学员管理',     'group:student',       1, 5),
  (6, NULL, '系统管理',     'group:system',        1, 6),
  (7, NULL, '学科管理',     'group:subject',       1, 7),
  (8, NULL, 'B站课程库',    'group:course',        1, 8),
  (9, NULL, '数据看板',     'group:dashboard',     1, 9),
  -- 1 题库管理（11~17）
  (11, 1, '查看', 'question:view',   2, 1),
  (12, 1, '新增', 'question:add',    2, 2),
  (13, 1, '编辑', 'question:edit',   2, 3),
  (14, 1, '删除', 'question:delete', 2, 4),
  (15, 1, '导入', 'question:import', 2, 5),
  (16, 1, '导出', 'question:export', 2, 6),
  (17, 1, '审核', 'question:audit',  2, 7),
  -- 2 面试题库管理（21~25）
  (21, 2, '查看',     'interviewbank:view',        2, 1),
  (22, 2, '上传',     'interviewbank:upload',      2, 2),
  (23, 2, '编辑',     'interviewbank:edit',        2, 3),
  (24, 2, '删除',     'interviewbank:delete',      2, 4),
  (25, 2, '检索测试', 'interviewbank:search_test', 2, 5),
  -- 3 用户管理（31~34）
  (31, 3, '查看', 'user:view',    2, 1),
  (32, 3, '新增', 'user:add',     2, 2),
  (33, 3, '编辑', 'user:edit',    2, 3),
  (34, 3, '禁用', 'user:disable', 2, 4),
  -- 4 学习路线（41~44）
  (41, 4, '查看', 'route:view',   2, 1),
  (42, 4, '新增', 'route:add',    2, 2),
  (43, 4, '编辑', 'route:edit',   2, 3),
  (44, 4, '删除', 'route:delete', 2, 4),
  -- 5 学员管理（51~53）
  (51, 5, '查看', 'student:view',   2, 1),
  (52, 5, '编辑', 'student:edit',   2, 2),
  (53, 5, '删除', 'student:delete', 2, 3),
  -- 6 系统管理（61~62）
  (61, 6, '日志查看', 'system:log_view', 2, 1),
  (62, 6, '系统设置', 'system:setting',  2, 2),
  -- 7 学科管理（71~74，D4-5 新增）
  (71, 7, '查看', 'subject:view',   2, 1),
  (72, 7, '新增', 'subject:add',    2, 2),
  (73, 7, '编辑', 'subject:edit',   2, 3),
  (74, 7, '删除', 'subject:delete', 2, 4),
  -- 8 B站课程库（75~78，D4-5 新增）
  (75, 8, '查看', 'course:view',   2, 1),
  (76, 8, '新增', 'course:add',    2, 2),
  (77, 8, '编辑', 'course:edit',   2, 3),
  (78, 8, '删除', 'course:delete', 2, 4),
  -- 9 数据看板（79，D4-5 新增）
  (79, 9, '查看', 'dashboard:view', 2, 1);

-- 6.4 角色-权限关联（只关联 type=2 操作点）
--     超级管理员 = 全部 34 项；
--     内容审核 = 查看全部内容 + 题目审核（共 7 项）：
--       question:view / question:audit / interviewbank:view / route:view
--       + D4-5 新增组查看 3 项 subject:view(71) / course:view(75) / dashboard:view(79)
--       （依据 03 §7「查看内容+题目审核」与 03 §7.2 ④「content_reviewer 补充 (2,71)(2,75)(2,79)」）；
--     运营人员 = 除用户管理组（user:view/add/edit/disable）、系统管理组（system:log_view / system:setting）
--                与题目审核（question:audit）外全部，共 27 项（运营人员不进入系统管理）。
INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`, `created_at`)
  SELECT 1, `id`, UTC_TIMESTAMP() FROM `permission` WHERE `type` = 2;

INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`, `created_at`)
  SELECT 2, `id`, UTC_TIMESTAMP() FROM `permission`
   WHERE `type` = 2
     AND `code` IN ('question:view', 'question:audit', 'interviewbank:view', 'route:view',
                    'subject:view', 'course:view', 'dashboard:view');

INSERT IGNORE INTO `role_permission` (`role_id`, `permission_id`, `created_at`)
  SELECT 3, `id`, UTC_TIMESTAMP() FROM `permission`
   WHERE `type` = 2
     AND `code` NOT IN ('user:view', 'user:add', 'user:edit', 'user:disable',
                        'system:log_view', 'system:setting', 'question:audit');

-- 6.5 系统设置 4 项（需求 4.5.4；除平台名称外为空值待运营填写）
INSERT IGNORE INTO `system_setting` (`id`, `config_key`, `config_value`, `remark`) VALUES
  (1, 'site_name',        '云枢智学', '平台名称（必填，显示在浏览器标题与 Logo 旁）'),
  (2, 'site_description', '',         '平台描述（多行文本，待运营填写）'),
  (3, 'logo_url',         '',         'Logo 图片地址（建议 256×256，待运营上传）'),
  (4, 'default_avatar',   '',         '默认头像地址（用户未设置头像时显示，待运营上传）');

-- 6.6 内置超级管理员（需求 4.5.1，唯一且不可删除 / 不可改角色）
--     ⚠️ password_hash 为占位符，不是有效 BCrypt 哈希：
--        部署初始化时必须重置为默认密码并生成有效哈希后再允许登录。
INSERT IGNORE INTO `admin_user`
  (`id`, `username`, `password_hash`, `mobile`, `email`, `role_id`, `status`, `is_builtin`) VALUES
  (1, 'admin', '__PLACEHOLDER_INVALID_HASH__RESET_AT_DEPLOYMENT__', NULL, NULL, 1, 1, 1);

-- =============================================================================
-- 脚本结束 · 24 张表 / 0 个物理外键 / 8 学科 / 3 角色 / 9 组 34 权限点 / 4 系统设置 / 1 内置超管
-- =============================================================================
