package com.yunshu.zhixue.modules.interviewbank.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicItem;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewTopicSaveRequest;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewTopic;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewQuestionMapper;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewTopicMapper;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewTopicService;
import com.yunshu.zhixue.modules.interviewbank.support.TopicCount;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 面试题主题词表服务实现。
 */
@Service
@RequiredArgsConstructor
public class InterviewTopicServiceImpl implements InterviewTopicService {

    private final InterviewTopicMapper topicMapper;
    private final InterviewQuestionMapper questionMapper;
    private final SubjectService subjectService;

    @Override
    public List<InterviewTopicItem> listTopics(Long subjectId) {
        Map<Long, String> subjectNames = subjectService.listOptions().stream()
                .collect(Collectors.toMap(SubjectOption::id, SubjectOption::name, (left, right) -> left));

        List<InterviewTopic> topics = topicMapper.selectList(Wrappers.<InterviewTopic>lambdaQuery()
                .and(subjectId != null, wrapper -> wrapper
                        .isNull(InterviewTopic::getSubjectId)
                        .or()
                        .eq(InterviewTopic::getSubjectId, subjectId))
                .orderByAsc(InterviewTopic::getSortOrder)
                .orderByAsc(InterviewTopic::getId));

        Map<String, Long> counts = new LinkedHashMap<>();
        for (TopicCount item : questionMapper.countByTopic(subjectId)) {
            if (StringUtils.hasText(item.getTopic())) {
                counts.put(item.getTopic(), item.getQuestionCount() == null ? 0L : item.getQuestionCount());
            }
        }

        Set<String> vocabulary = new HashSet<>();
        List<InterviewTopicItem> items = new ArrayList<>();
        for (InterviewTopic topic : topics) {
            vocabulary.add(topic.getName());
            items.add(new InterviewTopicItem(
                    topic.getId(),
                    topic.getName(),
                    topic.getSubjectId(),
                    topic.getSubjectId() == null ? null : subjectNames.get(topic.getSubjectId()),
                    topic.getSortOrder(),
                    counts.getOrDefault(topic.getName(), 0L),
                    true));
        }
        String currentSubjectName = subjectId == null ? null : subjectNames.get(subjectId);
        counts.entrySet().stream()
                .filter(entry -> !vocabulary.contains(entry.getKey()))
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> items.add(new InterviewTopicItem(
                        null,
                        entry.getKey(),
                        subjectId,
                        currentSubjectName,
                        0,
                        entry.getValue(),
                        false)));
        return items;
    }

    @Override
    public Long createTopic(InterviewTopicSaveRequest request) {
        Long subjectId = request.subjectId();
        requireSubjectIfPresent(subjectId);
        String name = request.name().trim();
        requireNameAvailable(name, subjectId, null);
        InterviewTopic topic = new InterviewTopic();
        topic.setName(name);
        topic.setSubjectId(subjectId);
        topic.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        topicMapper.insert(topic);
        return topic.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTopic(Long topicId, InterviewTopicSaveRequest request) {
        InterviewTopic topic = requireTopic(topicId);
        Long subjectId = request.subjectId();
        requireSubjectIfPresent(subjectId);
        String name = request.name().trim();
        requireNameAvailable(name, subjectId, topicId);

        String oldName = topic.getName();
        boolean scopeChanged = !Objects.equals(topic.getSubjectId(), subjectId);
        if (!oldName.equals(name) || scopeChanged) {
            var update = Wrappers.<InterviewQuestion>lambdaUpdate()
                    .set(InterviewQuestion::getTopic, name)
                    .eq(InterviewQuestion::getTopic, oldName);
            Long scope = topic.getSubjectId();
            if (scope != null) {
                update.eq(InterviewQuestion::getSubjectId, scope);
            }
            questionMapper.update(null, update);
        }
        topic.setName(name);
        topic.setSubjectId(subjectId);
        topic.setSortOrder(request.sortOrder() == null ? topic.getSortOrder() : request.sortOrder());
        topicMapper.updateById(topic);
    }

    @Override
    public void deleteTopic(Long topicId) {
        InterviewTopic topic = requireTopic(topicId);
        var countWrapper = Wrappers.<InterviewQuestion>lambdaQuery()
                .eq(InterviewQuestion::getTopic, topic.getName());
        if (topic.getSubjectId() != null) {
            countWrapper.eq(InterviewQuestion::getSubjectId, topic.getSubjectId());
        }
        long used = questionMapper.selectCount(countWrapper);
        if (used > 0) {
            throw BizException.of(ErrorCode.CONTENT_REFERENCED,
                    "该主题下仍有 " + used + " 道题目，请先把这些题目改到其他主题或清空主题");
        }
        topicMapper.deleteById(topicId);
    }

    private InterviewTopic requireTopic(Long topicId) {
        InterviewTopic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "主题不存在或已删除");
        }
        return topic;
    }

    private void requireSubjectIfPresent(Long subjectId) {
        if (subjectId == null) {
            return;
        }
        boolean exists = subjectService.listOptions().stream()
                .anyMatch(option -> option.id().equals(subjectId));
        if (!exists) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "所选学科不存在");
        }
    }

    private void requireNameAvailable(String name, Long subjectId, Long excludeId) {
        var wrapper = Wrappers.<InterviewTopic>lambdaQuery()
                .eq(InterviewTopic::getName, name)
                .eq(subjectId != null, InterviewTopic::getSubjectId, subjectId)
                .isNull(subjectId == null, InterviewTopic::getSubjectId);
        if (excludeId != null) {
            wrapper.ne(InterviewTopic::getId, excludeId);
        }
        if (topicMapper.selectCount(wrapper) > 0) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "同一学科下已存在同名主题");
        }
    }
}
