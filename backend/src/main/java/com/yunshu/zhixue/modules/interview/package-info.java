/**
 * AI 面试模块：面试配置、会话发起（Redis 配额与题目快照）、逐题 SSE 评分、报告生成与记录。
 *
 * <p>表归属：interview_record / interview_question_score / interview_report；
 * 发起阶段不落库，正常结束后单事务落库（D4-4）；
 * 逐题评分经 Redis 暂存防伪造，未完成会话仅存前端本地（D16）。</p>
 */
package com.yunshu.zhixue.modules.interview;
