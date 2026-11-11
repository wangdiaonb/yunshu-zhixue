/**
 * AI 能力层：OpenAI 兼容协议的模型客户端、提示词模板、简答批改、面试出题、评分与报告生成。
 *
 * <p>无状态、无业务库依赖：只接收入参与返回结构化结果，落库由调用方负责；
 * 只暴露领域语义接口（如 gradeAnswer / generateReport），模型细节不外泄；
 * 超时/重试后仍失败时降级并提示（架构 2.5、风险 R1）。</p>
 */
package com.yunshu.zhixue.modules.ai;
