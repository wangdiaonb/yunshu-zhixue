package com.yunshu.zhixue.modules.interviewbank.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionCreateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionDetail;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionQuery;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionUpdateRequest;

/**
 * 面试题库服务（A-I01 ~ A-I05、A-I15）。
 */
public interface InterviewQuestionService {

    PageResult<InterviewQuestionItem> pageQuestions(InterviewQuestionQuery query);

    InterviewQuestionDetail getQuestion(Long questionId);

    Long createQuestion(InterviewQuestionCreateRequest request);

    /** 仅主题与难度可修改。 */
    void updateQuestion(Long questionId, InterviewQuestionUpdateRequest request);

    void deleteQuestion(Long questionId);

    /** 上架（1）/ 下架（2）。 */
    void changeStatus(Long questionId, Integer status);
}
