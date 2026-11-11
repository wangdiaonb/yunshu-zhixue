package com.yunshu.zhixue.modules.interviewbank.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportConfirmResponse;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportErrorRow;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportPayload;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportPreviewResponse;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportRow;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportSubjectStat;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewTopic;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewQuestionMapper;
import com.yunshu.zhixue.modules.interviewbank.mapper.InterviewTopicMapper;
import com.yunshu.zhixue.modules.interviewbank.service.InterviewQuestionImportService;
import com.yunshu.zhixue.modules.interviewbank.support.InterviewQuestionExcelParser;
import com.yunshu.zhixue.modules.interviewbank.support.RawInterviewQuestionRow;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 面试题 Excel 导入服务实现：预览校验暂存 Redis，确认时一次性落库。
 */
@Service
@RequiredArgsConstructor
public class InterviewQuestionImportServiceImpl implements InterviewQuestionImportService {

    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private static final String PREVIEW_KEY_PREFIX = "interview:import:";
    private static final int PREVIEW_LIMIT = 50;
    private static final int MAX_TEXT_LENGTH = 10000;
    private static final int STATUS_ONLINE = 1;
    private static final int SOURCE_IMPORT = 3;

    private final InterviewQuestionExcelParser excelParser;
    private final InterviewQuestionMapper questionMapper;
    private final InterviewTopicMapper topicMapper;
    private final SubjectService subjectService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public InterviewQuestionImportPreviewResponse preview(MultipartFile file, Long defaultSubjectId) {
        List<SubjectOption> subjects = subjectService.listOptions();
        SubjectOption defaultSubject = resolveDefaultSubject(subjects, defaultSubjectId);
        Map<String, SubjectOption> subjectByName = subjects.stream()
                .collect(Collectors.toMap(option -> option.name().trim(), option -> option,
                        (left, right) -> left));

        List<RawInterviewQuestionRow> rawRows = excelParser.parse(file);
        List<InterviewQuestionImportRow> validRows = new ArrayList<>();
        List<InterviewQuestionImportErrorRow> errorRows = new ArrayList<>();

        for (RawInterviewQuestionRow raw : rawRows) {
            List<String> errors = new ArrayList<>();
            SubjectOption subject = resolveRowSubject(raw, subjectByName, defaultSubject, errors);
            String content = raw.content().trim();
            if (!StringUtils.hasText(content)) {
                errors.add("缺少题干");
            } else if (content.length() > MAX_TEXT_LENGTH) {
                errors.add("题干不能超过 " + MAX_TEXT_LENGTH + " 个字符");
            }
            int difficulty = parseDifficulty(raw.difficulty());
            if (difficulty == 0) {
                errors.add("难度仅支持 简单 / 中等 / 困难");
            }
            String topic = trimToNull(raw.topic());
            if (topic != null && topic.length() > 50) {
                errors.add("主题不能超过 50 个字符");
            }
            String tags = normalizeTags(raw.tags());
            if (tags != null && tags.length() > 255) {
                errors.add("标签不能超过 255 个字符");
            }
            String answer = trimToNull(raw.referenceAnswer());
            if (answer != null && answer.length() > MAX_TEXT_LENGTH) {
                errors.add("参考答案不能超过 " + MAX_TEXT_LENGTH + " 个字符");
            }
            if (!errors.isEmpty()) {
                errorRows.add(new InterviewQuestionImportErrorRow(raw.rowNumber(), content, errors));
                continue;
            }
            validRows.add(new InterviewQuestionImportRow(
                    raw.rowNumber(),
                    subject.id(),
                    subject.name(),
                    content,
                    topic,
                    difficulty,
                    tags,
                    answer));
        }

        List<InterviewQuestionImportRow> importableRows = removeDuplicates(validRows, errorRows);
        List<InterviewQuestionImportSubjectStat> subjectStats = summarizeSubjects(importableRows);
        List<String> topicsNotInVocabulary = listTopicsNotInVocabulary(importableRows);

        String token = null;
        if (!importableRows.isEmpty()) {
            token = UUID.randomUUID().toString().replace("-", "");
            String sourceDocName = trimToNull(file.getOriginalFilename());
            if (sourceDocName != null && sourceDocName.length() > 255) {
                sourceDocName = sourceDocName.substring(0, 255);
            }
            try {
                InterviewQuestionImportPayload payload =
                        new InterviewQuestionImportPayload(sourceDocName, importableRows);
                stringRedisTemplate.opsForValue().set(
                        PREVIEW_KEY_PREFIX + token,
                        objectMapper.writeValueAsString(payload),
                        PREVIEW_TTL);
            } catch (Exception ex) {
                throw BizException.of(ErrorCode.INTERNAL_ERROR, "导入预览暂存失败，请稍后重试");
            }
        }
        List<InterviewQuestionImportRow> previewRows = importableRows.size() <= PREVIEW_LIMIT
                ? importableRows
                : importableRows.subList(0, PREVIEW_LIMIT);
        return new InterviewQuestionImportPreviewResponse(
                token,
                rawRows.size(),
                importableRows.size(),
                errorRows.size(),
                subjectStats,
                topicsNotInVocabulary,
                previewRows,
                errorRows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InterviewQuestionImportConfirmResponse confirm(String token) {
        String key = PREVIEW_KEY_PREFIX + token;
        String json = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (!StringUtils.hasText(json)) {
            throw BizException.of(ErrorCode.NOT_FOUND, "导入预览已过期，请重新上传 Excel");
        }
        InterviewQuestionImportPayload payload;
        try {
            payload = objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.NOT_FOUND, "导入预览已失效，请重新上传 Excel");
        }
        if (payload.rows() == null || payload.rows().isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "没有可导入的面试题");
        }
        int seq = 0;
        for (InterviewQuestionImportRow row : payload.rows()) {
            InterviewQuestion question = new InterviewQuestion();
            question.setSubjectId(row.subjectId());
            question.setContent(row.content());
            question.setReferenceAnswer(row.referenceAnswer());
            question.setTopic(row.topic());
            question.setDifficulty(row.difficulty());
            question.setTags(row.tags());
            question.setSourceType(SOURCE_IMPORT);
            question.setSourceDocName(payload.sourceDocName());
            question.setSourceSeq(++seq);
            question.setStatus(STATUS_ONLINE);
            questionMapper.insert(question);
        }
        return new InterviewQuestionImportConfirmResponse(payload.rows().size());
    }

    @Override
    public byte[] template() {
        List<SubjectOption> subjects = subjectService.listOptions();
        String sampleSubject = subjects.isEmpty() ? "AI应用与智能体开发" : subjects.get(0).name();
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("面试题数据");
            String[] headers = {"所属学科", "题干", "难度", "主题", "标签", "参考答案"};
            int[] widths = {26, 48, 10, 22, 32, 60};
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            Row header = sheet.createRow(0);
            header.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                header.getCell(i).setCellStyle(headerStyle);
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            String[][] samples = {
                    {sampleSubject, "ollama 是什么，有什么作用？", "中等", "大模型与本地部署",
                            "Ollama, 本地部署, 性能优化, 部署",
                            "Ollama 是一个轻量级的本地大模型部署工具，允许用户快速在本地或服务器上部署和管理多个大模型。"
                                    + "其核心作用：1. 降低部署门槛；2. 支持多模型并行运行；3. 轻量化模型加载与推理。"},
                    {sampleSubject, "ollama 下载的模型是完整版还是阉割版？", "中等", "大模型与本地部署",
                            "Ollama, 性能优化",
                            "Ollama 提供了许多通过量化技术优化的轻量级模型版本，在大幅降低显存占用与提升运行效率的同时，"
                                    + "尽量保留了原模型的核心能力。"},
                    {sampleSubject, "你部署大模型的流程", "中等", "大模型与本地部署",
                            "Ollama, 本地部署, 模型微调, FastAPI",
                            "1. 环境准备：安装 CUDA、PyTorch、Docker；2. 模型选择：按任务选择模型；"
                                    + "3. 微调：在自有数据上训练；4. 服务化：通过 FastAPI 或 Ollama 部署为 API；"
                                    + "5. 监控优化：使用 Prometheus 与 Grafana 监控性能。"}
            };
            for (int rowIndex = 0; rowIndex < samples.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (int column = 0; column < samples[rowIndex].length; column++) {
                    row.createCell(column).setCellValue(samples[rowIndex][column]);
                }
            }
            sheet.createFreezePane(0, 1);

            Sheet notes = workbook.createSheet("填写说明");
            String[] noteLines = {
                    "1. 第一行表头不可删除或改名，示例行可在导入前删除。",
                    "2. 必填：所属学科、题干；难度为空时按「中等」处理。",
                    "3. 所属学科必须与系统「学科管理」中的学科名称完全一致，也支持按行混填不同学科。",
                    "4. 难度填写：简单 / 中等 / 困难（也可填 1 / 2 / 3），默认中等。",
                    "5. 主题可留空（未分类）；主题不在词表内时允许导入，会在预览中提示，可在「主题维护」中补充。",
                    "6. 标签多个用逗号分隔，例如：Ollama, 本地部署, 性能优化。",
                    "7. 参考答案可留空；支持单元格内换行（Alt + Enter）。",
                    "8. 单次最多导入 500 道，单文件不超过 10MB；同一学科下已存在的相同题干会被跳过。"
            };
            for (int i = 0; i < noteLines.length; i++) {
                notes.createRow(i).createCell(0).setCellValue(noteLines[i]);
            }
            notes.setColumnWidth(0, 110 * 256);

            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "生成面试题导入模板失败，请稍后重试");
        }
    }

    private SubjectOption resolveDefaultSubject(List<SubjectOption> subjects, Long defaultSubjectId) {
        if (defaultSubjectId == null) {
            return null;
        }
        return subjects.stream()
                .filter(option -> option.id().equals(defaultSubjectId))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.PARAM_INVALID, "默认学科不存在"));
    }

    private SubjectOption resolveRowSubject(RawInterviewQuestionRow raw,
                                            Map<String, SubjectOption> subjectByName,
                                            SubjectOption defaultSubject,
                                            List<String> errors) {
        String subjectName = trimToNull(raw.subjectName());
        if (subjectName == null) {
            if (defaultSubject == null) {
                errors.add("缺少所属学科，请填写学科名称或选择默认学科");
                return null;
            }
            return defaultSubject;
        }
        SubjectOption subject = subjectByName.get(subjectName);
        if (subject == null) {
            errors.add("所属学科「" + subjectName + "」不存在，请先在学科管理中创建");
        }
        return subject;
    }

    /**
     * 去除 Excel 内重复与库中已存在的题干（同一学科下），重复行转为错误明细。
     */
    private List<InterviewQuestionImportRow> removeDuplicates(List<InterviewQuestionImportRow> rows,
                                                             List<InterviewQuestionImportErrorRow> errorRows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        List<InterviewQuestionImportRow> uniqueRows = new ArrayList<>();
        for (InterviewQuestionImportRow row : rows) {
            String key = row.subjectId() + "\u0001" + row.content();
            if (seen.add(key)) {
                uniqueRows.add(row);
            } else {
                errorRows.add(new InterviewQuestionImportErrorRow(
                        row.rowNumber(), row.content(), List.of("Excel 内存在重复题干")));
            }
        }

        Map<Long, Set<String>> existing = new HashMap<>();
        Map<Long, List<String>> contentsBySubject = uniqueRows.stream()
                .collect(Collectors.groupingBy(InterviewQuestionImportRow::subjectId,
                        Collectors.mapping(InterviewQuestionImportRow::content, Collectors.toList())));
        contentsBySubject.forEach((subjectId, contents) -> {
            List<String> distinct = contents.stream().distinct().toList();
            List<InterviewQuestion> found = questionMapper.selectList(
                    Wrappers.<InterviewQuestion>lambdaQuery()
                            .select(InterviewQuestion::getContent)
                            .eq(InterviewQuestion::getSubjectId, subjectId)
                            .in(InterviewQuestion::getContent, distinct));
            existing.put(subjectId, found.stream()
                    .map(question -> question.getContent().trim())
                    .collect(Collectors.toSet()));
        });

        List<InterviewQuestionImportRow> importableRows = new ArrayList<>();
        for (InterviewQuestionImportRow row : uniqueRows) {
            if (existing.getOrDefault(row.subjectId(), Set.of()).contains(row.content())) {
                errorRows.add(new InterviewQuestionImportErrorRow(
                        row.rowNumber(), row.content(), List.of("题库中已存在相同题干")));
            } else {
                importableRows.add(row);
            }
        }
        return importableRows;
    }

    private List<InterviewQuestionImportSubjectStat> summarizeSubjects(List<InterviewQuestionImportRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, InterviewQuestionImportSubjectStat> stats = new LinkedHashMap<>();
        for (InterviewQuestionImportRow row : rows) {
            InterviewQuestionImportSubjectStat stat = stats.get(row.subjectId());
            stats.put(row.subjectId(), new InterviewQuestionImportSubjectStat(
                    row.subjectId(),
                    row.subjectName(),
                    stat == null ? 1 : stat.count() + 1));
        }
        return new ArrayList<>(stats.values());
    }

    private List<String> listTopicsNotInVocabulary(List<InterviewQuestionImportRow> rows) {
        Set<String> topics = rows.stream()
                .map(InterviewQuestionImportRow::topic)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (topics.isEmpty()) {
            return List.of();
        }
        Set<String> vocabulary = topicMapper.selectList(Wrappers.<InterviewTopic>lambdaQuery()
                        .select(InterviewTopic::getName)
                        .in(InterviewTopic::getName, topics))
                .stream()
                .map(InterviewTopic::getName)
                .collect(Collectors.toSet());
        return topics.stream().filter(topic -> !vocabulary.contains(topic)).toList();
    }

    private int parseDifficulty(String value) {
        String text = value == null ? "" : value.trim();
        return switch (text) {
            case "", "中等", "2" -> 2;
            case "简单", "1" -> 1;
            case "困难", "3" -> 3;
            default -> 0;
        };
    }

    private String normalizeTags(String value) {
        String text = trimToNull(value);
        if (text == null) {
            return null;
        }
        String joined = Arrays.stream(text.split("[,，、;；]"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
        return StringUtils.hasText(joined) ? joined : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
