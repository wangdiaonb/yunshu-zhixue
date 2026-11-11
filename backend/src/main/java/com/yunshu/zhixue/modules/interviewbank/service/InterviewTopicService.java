package com.yunshu.zhixue.modules.interviewbank.service;

import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicSaveRequest;

import java.util.List;

/**
 * 面试题主题词表服务（A-I10 ~ A-I13）。
 */
public interface InterviewTopicService {

    /**
     * 主题列表：词表主题（含 0 题） + 词表外主题（题库在用但未维护进词表）。
     *
     * @param subjectId 为空时返回全部主题
     */
    List<InterviewTopicItem> listTopics(Long subjectId);

    Long createTopic(InterviewTopicSaveRequest request);

    /** 名称变更时同步改写题目上的主题名，避免存量题目掉出词表。 */
    void updateTopic(Long topicId, InterviewTopicSaveRequest request);

    void deleteTopic(Long topicId);
}
