package com.yunshu.zhixue.modules.subject.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.subject.dto.StageItem;
import com.yunshu.zhixue.modules.subject.dto.StageSaveRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectCard;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.dto.SubjectSaveRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectStatsResponse;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.mapper.SubjectMapper;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectMapper subjectMapper;
    private final StageMapper stageMapper;

    @Override
    public SubjectStatsResponse stats() {
        long totalSubjects = subjectMapper.selectCount(null);
        long subjectsWithStages = subjectMapper.countSubjectsWithStages();
        long totalStages = stageMapper.selectCount(null);
        long totalQuestions = subjectMapper.countAllQuestions();
        return new SubjectStatsResponse(totalSubjects, subjectsWithStages, totalStages, totalQuestions);
    }

    @Override
    public List<SubjectOption> listOptions() {
        return subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                        .orderByAsc(Subject::getSortOrder)
                        .orderByAsc(Subject::getId))
                .stream()
                .map(subject -> new SubjectOption(subject.getId(), subject.getName(), subject.getIcon()))
                .toList();
    }

    @Override
    public List<SubjectCard> listSubjects() {
        List<Subject> subjects = subjectMapper.selectList(Wrappers.<Subject>lambdaQuery()
                .orderByAsc(Subject::getSortOrder)
                .orderByAsc(Subject::getId));
        List<Stage> stages = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .orderByAsc(Stage::getSortOrder)
                .orderByAsc(Stage::getId));
        Map<Long, List<Stage>> stagesBySubject = stages.stream()
                .collect(Collectors.groupingBy(Stage::getSubjectId));

        List<SubjectCard> cards = new ArrayList<>();
        for (Subject subject : subjects) {
            List<StageItem> stageItems = stagesBySubject.getOrDefault(subject.getId(), List.of()).stream()
                    .map(stage -> new StageItem(
                            stage.getId(),
                            stage.getName(),
                            stage.getIcon(),
                            stage.getSortOrder(),
                            stage.getStatus(),
                            stageMapper.countQuestions(stage.getId())))
                    .toList();
            cards.add(new SubjectCard(
                    subject.getId(),
                    subject.getName(),
                    subject.getIcon(),
                    subject.getDescription(),
                    subject.getThemeColor(),
                    subject.getSortOrder(),
                    subjectMapper.countCourses(subject.getId()),
                    subjectMapper.countRoutes(subject.getId()),
                    subjectMapper.countQuestions(subject.getId()),
                    stageItems));
        }
        return cards;
    }

    @Override
    public Long createSubject(SubjectSaveRequest request) {
        String name = request.name().trim();
        ensureSubjectNameAvailable(name, null);
        Subject subject = new Subject();
        subject.setName(name);
        subject.setIcon(request.icon().trim());
        subject.setDescription(request.description().trim());
        subject.setThemeColor(request.themeColor().trim());
        subject.setSortOrder(request.sortOrder());
        try {
            subjectMapper.insert(subject);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "学科名称已存在");
        }
        return subject.getId();
    }

    @Override
    public void updateSubject(Long subjectId, SubjectSaveRequest request) {
        Subject subject = requireSubject(subjectId);
        String name = request.name().trim();
        ensureSubjectNameAvailable(name, subjectId);
        subject.setName(name);
        subject.setIcon(request.icon().trim());
        subject.setDescription(request.description().trim());
        subject.setThemeColor(request.themeColor().trim());
        subject.setSortOrder(request.sortOrder());
        try {
            subjectMapper.updateById(subject);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "学科名称已存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortSubjects(List<Long> orderedIds) {
        List<Subject> subjects = subjectMapper.selectList(null);
        validateOrder(orderedIds, subjects.stream().map(Subject::getId).collect(Collectors.toSet()), "学科");
        for (int index = 0; index < orderedIds.size(); index++) {
            subjectMapper.update(null, Wrappers.<Subject>lambdaUpdate()
                    .eq(Subject::getId, orderedIds.get(index))
                    .set(Subject::getSortOrder, index + 1));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSubject(Long subjectId) {
        Subject subject = requireSubject(subjectId);
        long stageCount = stageMapper.selectCount(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId));
        long questionCount = subjectMapper.countQuestions(subjectId);
        long courseCount = subjectMapper.countCourses(subjectId);
        long routeCount = subjectMapper.countRoutes(subjectId);

        if (stageCount > 0 || questionCount > 0 || courseCount > 0 || routeCount > 0) {
            List<String> parts = new ArrayList<>();
            if (stageCount > 0) {
                parts.add(stageCount + " 个阶段");
            }
            if (questionCount > 0) {
                parts.add(questionCount + " 道题目");
            }
            if (courseCount > 0) {
                parts.add(courseCount + " 门课程");
            }
            if (routeCount > 0) {
                parts.add(routeCount + " 条路线");
            }
            throw BizException.of(ErrorCode.CONTENT_REFERENCED,
                    "学科「" + subject.getName() + "」下存在 " + String.join("、", parts)
                            + "，请先处理关联内容后再删除");
        }
        subjectMapper.deleteById(subjectId);
    }

    @Override
    public Long createStage(Long subjectId, StageSaveRequest request) {
        Subject subject = requireSubject(subjectId);
        String name = request.name().trim();
        ensureStageNameAvailable(subjectId, name, null);

        Stage stage = new Stage();
        stage.setSubjectId(subjectId);
        stage.setName(name);
        stage.setIcon(normalizeIcon(request.icon()));
        stage.setSortOrder(request.sortOrder() == null ? nextStageSort(subjectId) : request.sortOrder());
        stage.setStatus(1);
        try {
            stageMapper.insert(stage);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME,
                    "学科「" + subject.getName() + "」下阶段名称已存在");
        }
        return stage.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long ensureStage(Long subjectId, String stageName) {
        Subject subject = requireSubject(subjectId);
        String name = stageName == null ? "" : stageName.trim();
        if (name.isEmpty() || name.length() > 50) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "阶段名称需为 1~50 个字符");
        }
        Stage existing = stageMapper.selectOne(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .eq(Stage::getName, name)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }
        Stage stage = new Stage();
        stage.setSubjectId(subjectId);
        stage.setName(name);
        stage.setIcon(null);
        stage.setSortOrder(nextStageSort(subjectId));
        stage.setStatus(1);
        try {
            stageMapper.insert(stage);
            return stage.getId();
        } catch (DuplicateKeyException ex) {
            Stage concurrent = stageMapper.selectOne(Wrappers.<Stage>lambdaQuery()
                    .eq(Stage::getSubjectId, subjectId)
                    .eq(Stage::getName, name)
                    .last("LIMIT 1"));
            if (concurrent != null) {
                return concurrent.getId();
            }
            throw BizException.of(ErrorCode.DUPLICATE_NAME,
                    "学科「" + subject.getName() + "」下阶段名称已存在");
        }
    }

    @Override
    public void updateStage(Long subjectId, Long stageId, StageSaveRequest request) {
        Stage stage = requireStage(subjectId, stageId);
        String name = request.name().trim();
        ensureStageNameAvailable(subjectId, name, stageId);
        stage.setName(name);
        stage.setIcon(normalizeIcon(request.icon()));
        if (request.sortOrder() != null) {
            stage.setSortOrder(request.sortOrder());
        }
        try {
            stageMapper.updateById(stage);
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "该学科下阶段名称已存在");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortStages(Long subjectId, List<Long> orderedIds) {
        requireSubject(subjectId);
        List<Stage> stages = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId));
        validateOrder(orderedIds, stages.stream().map(Stage::getId).collect(Collectors.toSet()), "阶段");
        for (int index = 0; index < orderedIds.size(); index++) {
            stageMapper.update(null, Wrappers.<Stage>lambdaUpdate()
                    .eq(Stage::getId, orderedIds.get(index))
                    .set(Stage::getSortOrder, index + 1));
        }
    }

    @Override
    public void updateStageStatus(Long subjectId, Long stageId, Integer status) {
        requireStage(subjectId, stageId);
        stageMapper.update(null, Wrappers.<Stage>lambdaUpdate()
                .eq(Stage::getId, stageId)
                .eq(Stage::getSubjectId, subjectId)
                .set(Stage::getStatus, status));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStage(Long subjectId, Long stageId) {
        Stage stage = requireStage(subjectId, stageId);
        long questionCount = stageMapper.countQuestions(stageId);
        if (questionCount > 0) {
            throw BizException.of(ErrorCode.CONTENT_REFERENCED,
                    "阶段「" + stage.getName() + "」下存在 " + questionCount + " 道题目，请先处理题目后再删除");
        }
        stageMapper.deleteById(stageId);
    }

    private Subject requireSubject(Long subjectId) {
        Subject subject = subjectMapper.selectById(subjectId);
        if (subject == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "学科不存在");
        }
        return subject;
    }

    private Stage requireStage(Long subjectId, Long stageId) {
        Stage stage = stageMapper.selectOne(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getId, stageId)
                .eq(Stage::getSubjectId, subjectId)
                .last("LIMIT 1"));
        if (stage == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "阶段不存在");
        }
        return stage;
    }

    private void ensureSubjectNameAvailable(String name, Long excludeId) {
        Long count = subjectMapper.selectCount(Wrappers.<Subject>lambdaQuery()
                .eq(Subject::getName, name)
                .ne(excludeId != null, Subject::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "学科名称已存在");
        }
    }

    private void ensureStageNameAvailable(Long subjectId, String name, Long excludeId) {
        Long count = stageMapper.selectCount(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .eq(Stage::getName, name)
                .ne(excludeId != null, Stage::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "该学科下阶段名称已存在");
        }
    }

    private int nextStageSort(Long subjectId) {
        Stage last = stageMapper.selectOne(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subjectId)
                .orderByDesc(Stage::getSortOrder)
                .orderByDesc(Stage::getId)
                .last("LIMIT 1"));
        return last == null || last.getSortOrder() == null ? 1 : last.getSortOrder() + 1;
    }

    private void validateOrder(List<Long> orderedIds, Set<Long> expectedIds, String label) {
        Set<Long> provided = new LinkedHashSet<>(orderedIds);
        if (provided.size() != orderedIds.size() || !Objects.equals(provided, expectedIds)) {
            throw BizException.of(ErrorCode.PARAM_INVALID,
                    "排序列表与现有" + label + "不一致，请刷新后重试");
        }
    }

    private String normalizeIcon(String icon) {
        return StringUtils.hasText(icon) ? icon.trim() : null;
    }
}
