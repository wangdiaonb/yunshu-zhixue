package com.yunshu.zhixue.modules.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.question.dto.QuestionAuditRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchDeleteResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionBatchStatusResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionDeleteResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionDetail;
import com.yunshu.zhixue.modules.question.dto.QuestionItem;
import com.yunshu.zhixue.modules.question.dto.QuestionOptionItem;
import com.yunshu.zhixue.modules.question.dto.QuestionOptionSave;
import com.yunshu.zhixue.modules.question.dto.QuestionQuery;
import com.yunshu.zhixue.modules.question.dto.QuestionSaveRequest;
import com.yunshu.zhixue.modules.question.dto.QuestionStatsResponse;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import com.yunshu.zhixue.modules.question.mapper.QuestionMapper;
import com.yunshu.zhixue.modules.question.mapper.QuestionOptionMapper;
import com.yunshu.zhixue.modules.question.service.QuestionService;
import com.yunshu.zhixue.modules.question.support.QuestionPracticeStat;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import com.yunshu.zhixue.modules.subject.mapper.StageMapper;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final int MAX_EXPORT_ROWS = 10000;

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final StageMapper stageMapper;
    private final SubjectService subjectService;

    @Override
    public QuestionStatsResponse stats() {
        return new QuestionStatsResponse(
                questionMapper.selectCount(null),
                countByStatus(1),
                countByStatus(3),
                countByType(1),
                countByType(2),
                countByType(3),
                countByType(4));
    }

    @Override
    public PageResult<QuestionItem> pageQuestions(QuestionQuery query) {
        Page<Question> page = new Page<>(query.pageOrDefault(), query.pageSizeOrDefault());
        LambdaQueryWrapper<Question> wrapper = buildQueryWrapper(query);
        if (wrapper == null) {
            return PageResult.of(List.of(), 0, page.getCurrent(), page.getSize());
        }
        IPage<Question> result = questionMapper.selectPage(page, wrapper);
        List<Question> questions = result.getRecords();
        if (questions.isEmpty()) {
            return PageResult.of(List.of(), result.getTotal(), result.getCurrent(), result.getSize());
        }
        Map<Long, Stage> stageMap = loadStages(questions);
        Map<Long, SubjectOption> subjectMap = subjectService.listOptions().stream()
                .collect(Collectors.toMap(SubjectOption::id, Function.identity(), (left, right) -> left));
        Map<Long, QuestionPracticeStat> statMap = loadPracticeStats(
                questions.stream().map(Question::getId).toList());
        List<QuestionItem> items = questions.stream()
                .map(question -> toItem(question, stageMap, subjectMap, statMap.get(question.getId())))
                .toList();
        return PageResult.of(items, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public QuestionDetail getQuestion(Long questionId) {
        Question question = requireQuestion(questionId);
        Stage stage = stageMapper.selectById(question.getStageId());
        SubjectOption subject = stage == null ? null : subjectById(stage.getSubjectId());
        List<QuestionOptionItem> options = loadOptions(questionId).stream()
                .map(option -> new QuestionOptionItem(
                        option.getId(),
                        option.getOptionKey(),
                        option.getOptionText(),
                        option.getIsCorrect() != null && option.getIsCorrect() == 1,
                        option.getSortOrder()))
                .toList();
        QuestionPracticeStat stat = loadPracticeStats(List.of(questionId)).get(questionId);
        long doneCount = stat == null || stat.getDoneCount() == null ? 0 : stat.getDoneCount();
        long correctCount = stat == null || stat.getCorrectCount() == null ? 0 : stat.getCorrectCount();
        return new QuestionDetail(
                question.getId(),
                question.getContent(),
                question.getType(),
                question.getDifficulty(),
                question.getStatus(),
                question.getStageId(),
                stage == null ? "未知阶段" : stage.getName(),
                stage == null ? null : stage.getSubjectId(),
                subject == null ? "未知学科" : subject.name(),
                question.getTags(),
                question.getAnswer(),
                question.getAnalysis(),
                question.getRejectReason(),
                options,
                doneCount,
                correctCount,
                accuracy(stat),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQuestion(QuestionSaveRequest request) {
        requireStage(request.stageId());
        NormalizedQuestion normalized = normalizeQuestion(request);
        Question question = new Question();
        applyFields(question, request, normalized);
        question.setStatus(3);
        question.setRejectReason(null);
        questionMapper.insert(question);
        insertOptions(question.getId(), normalized.options());
        return question.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestion(Long questionId, QuestionSaveRequest request) {
        Question question = requireQuestion(questionId);
        requireStage(request.stageId());
        NormalizedQuestion normalized = normalizeQuestion(request);
        applyFields(question, request, normalized);
        int currentStatus = question.getStatus() == null ? 3 : question.getStatus();
        if (currentStatus == 3 || currentStatus == 5) {
            question.setStatus(3);
            question.setRejectReason(null);
        } else {
            question.setStatus(currentStatus);
        }
        questionMapper.updateById(question);
        questionOptionMapper.deletePhysicalByQuestion(questionId);
        insertOptions(questionId, normalized.options());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionDeleteResponse deleteQuestion(Long questionId) {
        requireQuestion(questionId);
        long refCount = questionMapper.countReferences(questionId);
        questionMapper.deleteById(questionId);
        questionOptionMapper.deletePhysicalByQuestion(questionId);
        return new QuestionDeleteResponse(refCount);
    }

    @Override
    public void auditQuestion(Long questionId, QuestionAuditRequest request) {
        Question question = requireQuestion(questionId);
        if (question.getStatus() == null || question.getStatus() != 3) {
            throw BizException.of(ErrorCode.INVALID_STATE_TRANSITION, "当前状态不可审核");
        }
        if (Boolean.TRUE.equals(request.pass())) {
            question.setStatus(1);
            question.setRejectReason(null);
        } else {
            String reason = trimToNull(request.rejectReason());
            if (reason == null) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "审核不通过时必须填写驳回原因");
            }
            question.setStatus(5);
            question.setRejectReason(reason);
        }
        questionMapper.updateById(question);
    }

    @Override
    public void changeStatus(Long questionId, String action) {
        Question question = requireQuestion(questionId);
        int current = question.getStatus() == null ? 0 : question.getStatus();
        if ("off".equals(action)) {
            if (current != 1) {
                throw BizException.of(ErrorCode.INVALID_STATE_TRANSITION, "仅已上架题目可以下架");
            }
            question.setStatus(2);
        } else if ("on".equals(action)) {
            if (current != 2) {
                throw BizException.of(ErrorCode.INVALID_STATE_TRANSITION, "仅已下架题目可以重新上架");
            }
            question.setStatus(3);
        } else {
            throw BizException.of(ErrorCode.PARAM_INVALID, "操作参数不合法");
        }
        questionMapper.updateById(question);
    }

    @Override
    public void resubmit(Long questionId) {
        Question question = requireQuestion(questionId);
        if (question.getStatus() == null || question.getStatus() != 5) {
            throw BizException.of(ErrorCode.INVALID_STATE_TRANSITION, "仅已驳回题目可以重新提交");
        }
        question.setStatus(3);
        question.setRejectReason(null);
        questionMapper.updateById(question);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionBatchStatusResponse batchChangeStatus(List<Long> ids, String action) {
        List<Question> questions = questionMapper.selectBatchIds(ids);
        int updated = 0;
        int skipped = 0;
        for (Question question : questions) {
            int current = question.getStatus() == null ? 0 : question.getStatus();
            if ("on".equals(action) && current == 2) {
                question.setStatus(3);
                questionMapper.updateById(question);
                updated++;
            } else if ("off".equals(action) && current == 1) {
                question.setStatus(2);
                questionMapper.updateById(question);
                updated++;
            } else {
                skipped++;
            }
        }
        skipped += ids.size() - questions.size();
        return new QuestionBatchStatusResponse(updated, skipped);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionBatchDeleteResponse batchDelete(List<Long> ids) {
        List<Question> questions = questionMapper.selectBatchIds(ids);
        int deleted = 0;
        long refCount = 0;
        for (Question question : questions) {
            refCount += questionMapper.countReferences(question.getId());
            questionMapper.deleteById(question.getId());
            questionOptionMapper.deletePhysicalByQuestion(question.getId());
            deleted++;
        }
        return new QuestionBatchDeleteResponse(deleted, refCount);
    }

    @Override
    public byte[] exportQuestions(QuestionQuery query) {
        LambdaQueryWrapper<Question> wrapper = buildQueryWrapper(query);
        List<Question> questions = wrapper == null
                ? List.of()
                : questionMapper.selectList(wrapper);
        if (questions.size() > MAX_EXPORT_ROWS) {
            throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED,
                    "导出数据超过 10000 条，请缩小筛选范围");
        }
        Map<Long, Stage> stageMap = loadStages(questions);
        Map<Long, SubjectOption> subjectMap = subjectService.listOptions().stream()
                .collect(Collectors.toMap(SubjectOption::id, Function.identity(), (left, right) -> left));
        Map<Long, List<QuestionOption>> optionMap = loadOptionsForQuestions(
                questions.stream().map(Question::getId).toList());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("题库导出");
            String[] headers = {"ID", "题干", "阶段", "学科", "题型", "难度",
                    "选项A", "选项B", "选项C", "选项D", "选项E", "选项F",
                    "正确答案", "解析", "标签", "状态"};
            CellStyle headerStyle = headerStyle(workbook);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (Question question : questions) {
                Stage stage = stageMap.get(question.getStageId());
                SubjectOption subject = stage == null ? null : subjectMap.get(stage.getSubjectId());
                List<QuestionOption> options = optionMap.getOrDefault(question.getId(), List.of());
                Map<String, String> optionTexts = options.stream()
                        .collect(Collectors.toMap(QuestionOption::getOptionKey,
                                QuestionOption::getOptionText, (left, right) -> left));
                String answer = correctAnswer(question, options);
                Row row = sheet.createRow(rowIndex++);
                int column = 0;
                row.createCell(column++).setCellValue(question.getId());
                row.createCell(column++).setCellValue(question.getContent());
                row.createCell(column++).setCellValue(stage == null ? "未知阶段" : stage.getName());
                row.createCell(column++).setCellValue(subject == null ? "未知学科" : subject.name());
                row.createCell(column++).setCellValue(typeLabel(question.getType()));
                row.createCell(column++).setCellValue(difficultyLabel(question.getDifficulty()));
                for (String key : List.of("A", "B", "C", "D", "E", "F")) {
                    row.createCell(column++).setCellValue(optionTexts.getOrDefault(key, ""));
                }
                row.createCell(column++).setCellValue(answer);
                row.createCell(column++).setCellValue(question.getAnalysis() == null ? "" : question.getAnalysis());
                row.createCell(column++).setCellValue(question.getTags() == null ? "" : question.getTags());
                row.createCell(column).setCellValue(statusLabel(question.getStatus()));
            }
            int[] widths = {8, 50, 18, 22, 10, 10, 20, 20, 20, 20, 20, 20, 24, 40, 20, 10};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "导出题库失败，请稍后重试");
        }
    }

    private LambdaQueryWrapper<Question> buildQueryWrapper(QuestionQuery query) {
        LambdaQueryWrapper<Question> wrapper = Wrappers.<Question>lambdaQuery();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(condition -> condition
                    .like(Question::getContent, keyword)
                    .or()
                    .like(Question::getTags, keyword));
        }
        if (query.getSubjectId() != null) {
            List<Long> stageIds = stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                            .eq(Stage::getSubjectId, query.getSubjectId()))
                    .stream()
                    .map(Stage::getId)
                    .toList();
            if (stageIds.isEmpty()) {
                return null;
            }
            wrapper.in(Question::getStageId, stageIds);
        }
        if (query.getStageId() != null) {
            wrapper.eq(Question::getStageId, query.getStageId());
        }
        if (query.getType() != null) {
            wrapper.eq(Question::getType, query.getType());
        }
        if (query.getDifficulty() != null) {
            wrapper.eq(Question::getDifficulty, query.getDifficulty());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Question::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Question::getId);
        return wrapper;
    }

    private QuestionItem toItem(Question question, Map<Long, Stage> stageMap,
                                Map<Long, SubjectOption> subjectMap, QuestionPracticeStat stat) {
        Stage stage = stageMap.get(question.getStageId());
        SubjectOption subject = stage == null ? null : subjectMap.get(stage.getSubjectId());
        return new QuestionItem(
                question.getId(),
                question.getContent(),
                question.getType(),
                question.getDifficulty(),
                question.getStatus(),
                question.getStageId(),
                stage == null ? "未知阶段" : stage.getName(),
                stage == null ? null : stage.getSubjectId(),
                subject == null ? "未知学科" : subject.name(),
                question.getTags(),
                question.getRejectReason(),
                question.getCreatedAt(),
                stat == null || stat.getDoneCount() == null ? 0 : stat.getDoneCount(),
                accuracy(stat));
    }

    private void applyFields(Question question, QuestionSaveRequest request, NormalizedQuestion normalized) {
        question.setStageId(request.stageId());
        question.setType(request.type());
        question.setDifficulty(request.difficulty());
        question.setContent(request.content().trim());
        question.setTags(trimToNull(request.tags()));
        question.setAnalysis(trimToNull(request.analysis()));
        question.setAnswer(normalized.answer());
    }

    private NormalizedQuestion normalizeQuestion(QuestionSaveRequest request) {
        int type = request.type();
        if (type == 1 || type == 2) {
            List<QuestionOptionSave> options = request.options() == null
                    ? List.of()
                    : request.options().stream()
                    .filter(Objects::nonNull)
                    .toList();
            if (options.size() < 2 || options.size() > 6) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "单选/多选题需设置 2~6 个选项");
            }
            for (QuestionOptionSave option : options) {
                if (!StringUtils.hasText(option.text())) {
                    throw BizException.of(ErrorCode.PARAM_INVALID, "选项内容不能为空");
                }
            }
            long correct = options.stream()
                    .filter(option -> Boolean.TRUE.equals(option.correct()))
                    .count();
            if (type == 1 && correct != 1) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "单选题必须且只能有 1 个正确选项");
            }
            if (type == 2 && correct < 1) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "多选题至少需要 1 个正确选项");
            }
            return new NormalizedQuestion(null, options);
        }
        if (type == 3) {
            String answer = normalizeJudgeAnswer(request.answer());
            if (answer == null) {
                throw BizException.of(ErrorCode.PARAM_INVALID, "判断题答案必须为「正确」或「错误」");
            }
            return new NormalizedQuestion(answer, List.of());
        }
        String answer = trimToNull(request.answer());
        if (answer == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "简答题必须填写参考答案");
        }
        return new NormalizedQuestion(answer, List.of());
    }

    private String normalizeJudgeAnswer(String value) {
        String answer = trimToNull(value);
        if (answer == null) {
            return null;
        }
        return switch (answer) {
            case "正确", "对", "true", "True", "1" -> "正确";
            case "错误", "错", "false", "False", "0" -> "错误";
            default -> null;
        };
    }

    private void insertOptions(Long questionId, List<QuestionOptionSave> options) {
        for (int index = 0; index < options.size(); index++) {
            QuestionOptionSave option = options.get(index);
            QuestionOption entity = new QuestionOption();
            entity.setQuestionId(questionId);
            entity.setOptionKey(String.valueOf((char) ('A' + index)));
            entity.setOptionText(option.text().trim());
            entity.setIsCorrect(Boolean.TRUE.equals(option.correct()) ? 1 : 0);
            entity.setSortOrder(index + 1);
            questionOptionMapper.insert(entity);
        }
    }

    private void requireStage(Long stageId) {
        if (stageMapper.selectById(stageId) == null) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "所选阶段不存在");
        }
    }

    private Question requireQuestion(Long questionId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null) {
            throw BizException.of(ErrorCode.NOT_FOUND, "题目不存在");
        }
        return question;
    }

    private Map<Long, Stage> loadStages(List<Question> questions) {
        Set<Long> stageIds = questions.stream()
                .map(Question::getStageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (stageIds.isEmpty()) {
            return Map.of();
        }
        return stageMapper.selectBatchIds(stageIds).stream()
                .collect(Collectors.toMap(Stage::getId, Function.identity(), (left, right) -> left));
    }

    private Map<Long, QuestionPracticeStat> loadPracticeStats(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Map.of();
        }
        return questionMapper.selectPracticeStats(questionIds).stream()
                .collect(Collectors.toMap(QuestionPracticeStat::getQuestionId,
                        Function.identity(), (left, right) -> left));
    }

    private List<QuestionOption> loadOptions(Long questionId) {
        return questionOptionMapper.selectList(Wrappers.<QuestionOption>lambdaQuery()
                .eq(QuestionOption::getQuestionId, questionId)
                .orderByAsc(QuestionOption::getSortOrder)
                .orderByAsc(QuestionOption::getId));
    }

    private Map<Long, List<QuestionOption>> loadOptionsForQuestions(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        return questionOptionMapper.selectList(Wrappers.<QuestionOption>lambdaQuery()
                        .in(QuestionOption::getQuestionId, questionIds)
                        .orderByAsc(QuestionOption::getSortOrder)
                        .orderByAsc(QuestionOption::getId))
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
    }

    private SubjectOption subjectById(Long subjectId) {
        if (subjectId == null) {
            return null;
        }
        return subjectService.listOptions().stream()
                .filter(option -> option.id().equals(subjectId))
                .findFirst()
                .orElse(null);
    }

    private double accuracy(QuestionPracticeStat stat) {
        if (stat == null || stat.getDoneCount() == null || stat.getDoneCount() == 0) {
            return 0.0;
        }
        long correct = stat.getCorrectCount() == null ? 0 : stat.getCorrectCount();
        return BigDecimal.valueOf(correct * 100.0 / stat.getDoneCount())
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private long countByStatus(int status) {
        return questionMapper.selectCount(Wrappers.<Question>lambdaQuery()
                .eq(Question::getStatus, status));
    }

    private long countByType(int type) {
        return questionMapper.selectCount(Wrappers.<Question>lambdaQuery()
                .eq(Question::getType, type));
    }

    private String correctAnswer(Question question, List<QuestionOption> options) {
        if (question.getType() == 1 || question.getType() == 2) {
            return options.stream()
                    .filter(option -> option.getIsCorrect() != null && option.getIsCorrect() == 1)
                    .map(QuestionOption::getOptionKey)
                    .sorted()
                    .collect(Collectors.joining(","));
        }
        return question.getAnswer() == null ? "" : question.getAnswer();
    }

    private String typeLabel(Integer type) {
        return switch (type == null ? 0 : type) {
            case 1 -> "单选";
            case 2 -> "多选";
            case 3 -> "判断";
            case 4 -> "简答";
            default -> "";
        };
    }

    private String difficultyLabel(Integer difficulty) {
        return switch (difficulty == null ? 0 : difficulty) {
            case 1 -> "简单";
            case 2 -> "中等";
            case 3 -> "困难";
            default -> "";
        };
    }

    private String statusLabel(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "已上架";
            case 2 -> "已下架";
            case 3 -> "待审核";
            case 5 -> "已驳回";
            default -> "草稿";
        };
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        return style;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record NormalizedQuestion(String answer, List<QuestionOptionSave> options) {
    }
}
