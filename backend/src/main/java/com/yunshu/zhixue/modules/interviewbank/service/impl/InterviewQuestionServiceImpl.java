package com.yunshu.zhixue.modules.interviewbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionCreateRequest;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionDetail;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionQuery;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionUpdateRequest;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewTopic;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewQuestionMapper;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewTopicMapper;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewQuestionService;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 面试题库服务实现。
 */
@Service
@RequiredArgsConstructor
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    /** 来源：手工录入。 */
    private static final int SOURCE_MANUAL = 2;
    private static final int STATUS_ONLINE = 1;

    private final InterviewQuestionMapper questionMapper;
    private final InterviewTopicMapper topicMapper;
    private final SubjectService subjectService;

    @Override
    public PageResult<InterviewQuestionItem> pageQuestions(InterviewQuestionQuery query) {
        long pageNo = query.pageOrDefault();
        long pageSize = query.pageSizeOrDefault();
        boolean unclassified = Boolean.TRUE.equals(query.getUnclassified());
        boolean hasTopicFilter = query.getTopicId() != null || StringUtils.hasText(query.getTopic());
        if (unclassified && hasTopicFilter) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "「只看未分类」与主题筛选不能同时使用");
        }

        LambdaQueryWrapper<InterviewQuestion> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(condition -> condition
                    .like(InterviewQuestion::getContent, keyword)
                    .or()
                    .like(InterviewQuestion::getTags, keyword));
        }
        if (query.getSubjectId() != null) {
            wrapper.eq(InterviewQuestion::getSubjectId, query.getSubjectId());
        }
        if (unclassified) {
            wrapper.and(condition -> condition
                    .isNull(InterviewQuestion::getTopic)
                    .or()
                    .eq(InterviewQuestion::getTopic, ""));
        } else if (query.getTopicId() != null) {
            InterviewTopic topic = topicMapper.selectById(query.getTopicId());
            if (topic == null) {
                return PageResult.empty(pageNo, pageSize);
            }
            wrapper.eq(InterviewQuestion::getTopic, topic.getName());
        } else if (StringUtils.hasText(query.getTopic())) {
            wrapper.eq(InterviewQuestion::getTopic, query.getTopic().trim());
        }
        if (query.getDifficulty() != null) {
            wrapper.eq(InterviewQuestion::getDifficulty, query.getDifficulty());
        }
        if (query.getStatus() != null) {
            wrapper.eq(InterviewQuestion::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(InterviewQuestion::getId);

        IPage<InterviewQuestion> result = questionMapper.selectPage(new Page<>(pageNo, pageSize), wrapper);
        List<InterviewQuestion> questions = result.getRecords();
        if (questions.isEmpty()) {
            return PageResult.of(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        Map<Long, String> subjectNames = subjectNames();
        List<InterviewQuestionItem> items = questions.stream()
                .map(question -> toItem(question, subjectNames))
                .toList();
        return PageResult.of(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public InterviewQuestionDetail getQuestion(Long questionId) {
        InterviewQuestion question = requireQuestion(questionId);
        return new InterviewQuestionDetail(
                question.getId(),
                question.getSubjectId(),
                subjectName(question.getSubjectId()),
                question.getContent(),
                question.getReferenceAnswer(),
                question.getTopic(),
                question.getDifficulty(),
                question.getTags(),
                question.getSourceType(),
                question.getSourceDocName(),
                question.getSourceSeq(),
                question.getStatus(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    @Override
    public Long createQuestion(InterviewQuestionCreateRequest request) {
        requireSubject(request.subjectId());
        InterviewQuestion question = new InterviewQuestion();
        question.setSubjectId(request.subjectId());
        question.setContent(request.content().trim());
        question.setReferenceAnswer(trimToNull(request.referenceAnswer()));
        question.setTopic(trimToNull(request.topic()));
        question.setDifficulty(request.difficulty() == null ? 2 : request.difficulty());
        question.setTags(normalizeTags(request.tags()));
        question.setSourceType(SOURCE_MANUAL);
        question.setSourceDocName(null);
        question.setSourceSeq(null);
        question.setStatus(STATUS_ONLINE);
        questionMapper.insert(question);
        return question.getId();
    }

    @Override
    public void updateQuestion(Long questionId, InterviewQuestionUpdateRequest request) {
        InterviewQuestion question = requireQuestion(questionId);
        question.setTopic(trimToNull(request.topic()));
        question.setDifficulty(request.difficulty());
        questionMapper.updateById(question);
    }

    @Override
    public void deleteQuestion(Long questionId) {
        requireQuestion(questionId);
        questionMapper.deleteById(questionId);
    }

    @Override
    public void changeStatus(Long questionId, Integer status) {
        InterviewQuestion question = requireQuestion(questionId);
        question.setStatus(status);
        questionMapper.updateById(question);
    }

    private InterviewQuestionItem toItem(InterviewQuestion question, Map<Long, String> subjectNames) {
        return new InterviewQuestionItem(
                question.getId(),
                question.getSubjectId(),
                subjectName(question.getSubjectId(), subjectNames),
                question.getContent(),
                question.getTopic(),
                question.getDifficulty(),
                question.getTags(),
                question.getSourceType(),
                question.getSourceDocName(),
                question.getSourceSeq(),
                question.getStatus(),
                question.getCreatedAt());
    }

    private InterviewQuestion requireQuestion(Long questionId) {
        InterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "面试题不存在或已删除");
        }
        return question;
    }

    private void requireSubject(Long subjectId) {
        boolean exists = subjectService.listOptions().stream()
                .anyMatch(option -> option.id().equals(subjectId));
        if (!exists) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "所选学科不存在");
        }
    }

    private Map<Long, String> subjectNames() {
        return subjectService.listOptions().stream()
                .collect(Collectors.toMap(SubjectOption::id, SubjectOption::name, (left, right) -> left));
    }

    private String subjectName(Long subjectId) {
        return subjectName(subjectId, subjectNames());
    }

    private String subjectName(Long subjectId, Map<Long, String> subjectNames) {
        if (subjectId == null) {
            return "未知学科";
        }
        return subjectNames.getOrDefault(subjectId, "未知学科");
    }

    private String normalizeTags(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        return Arrays.stream(text.split("[,，、;；]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
