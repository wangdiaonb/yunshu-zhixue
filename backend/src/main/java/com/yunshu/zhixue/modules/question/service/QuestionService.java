package com.yunshu.zhixue.modules.question.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.dto.QuestionAuditRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchDeleteResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchStatusResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionDeleteResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionDetail;
import com.yunshu.zhixue.modules.question.dto.QuestionItem;
import com.yunshu.zhixue.modules.question.dto.QuestionQuery;
import com.yunshu.zhixue.modules.question.dto.QuestionSaveRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionStatsResponse;

import java.util.List;

/**
 * 题库管理服务（A-Q01 ~ A-Q11、A-Q15）。
 */
public interface QuestionService {

    QuestionStatsResponse stats();

    PageResult<QuestionItem> pageQuestions(QuestionQuery query);

    QuestionDetail getQuestion(Long questionId);

    Long createQuestion(QuestionSaveRequest request);

    void updateQuestion(Long questionId, QuestionSaveRequest request);

    QuestionDeleteResponse deleteQuestion(Long questionId);

    void auditQuestion(Long questionId, QuestionAuditRequest request);

    void changeStatus(Long questionId, String action);

    void resubmit(Long questionId);

    QuestionBatchStatusResponse batchChangeStatus(List<Long> ids, String action);

    QuestionBatchDeleteResponse batchDelete(List<Long> ids);

    /** 按当前筛选导出 Excel（最多 10000 行）。 */
    byte[] exportQuestions(QuestionQuery query);
}
