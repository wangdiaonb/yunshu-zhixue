package com.yunshu.zhixue.modules.question.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.question.dto.QuestionImportConfirmResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionImportErrorRow;
import com.yunshu.zhixue.modules.question.dto.QuestionImportOption;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPayload;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPreviewResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionImportRow;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import com.yunshu.zhixue.modules.question.mapper.QuestionMapper;
import com.yunshu.zhixue.modules.question.mapper.QuestionOptionMapper;
import com.yunshu.zhixue.modules.question.service.QuestionImportService;
import com.yunshu.zhixue.modules.question.support.QuestionExcelParser;
import com.yunshu.zhixue.modules.question.support.RawQuestionRow;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QuestionImportServiceImpl implements QuestionImportService {

    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private static final String PREVIEW_KEY_PREFIX = "question:import:";
    private static final int PREVIEW_LIMIT = 50;

    private final QuestionExcelParser excelParser;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final StageMapper stageMapper;
    private final SubjectService subjectService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public QuestionImportPreviewResponse preview(MultipartFile file, Long subjectId) {
        SubjectOption subject = subjectService.listOptions().stream()
                .filter(option -> option.id().equals(subjectId))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.PARAM_INVALID, "请选择有效的目标学科"));
        List<RawQuestionRow> rawRows = excelParser.parse(file);
        Map<String, Long> existingStages = new HashMap<>();
        for (Stage stage : stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subject.id()))) {
            existingStages.putIfAbsent(stage.getName().trim(), stage.getId());
        }

        Set<String> stagesToCreate = new LinkedHashSet<>();
        List<QuestionImportRow> validRows = new ArrayList<>();
        List<QuestionImportErrorRow> errorRows = new ArrayList<>();

        for (RawQuestionRow raw : rawRows) {
            List<String> errors = new ArrayList<>();
            String content = raw.content().trim();
            if (!StringUtils.hasText(content)) {
                errors.add("缺少题干");
            } else if (content.length() > 10000) {
                errors.add("题干过长");
            }
            String stageName = raw.category().trim();
            if (!StringUtils.hasText(stageName)) {
                errors.add("缺少分类/阶段");
            } else if (stageName.length() > 50) {
                errors.add("分类/阶段名称不能超过 50 个字符");
            } else if (!existingStages.containsKey(stageName)) {
                stagesToCreate.add(stageName);
            }

            int type = parseType(raw.type());
            if (type == 0) {
                errors.add("题型仅支持 单选/多选/判断/简答");
            }
            int difficulty = parseDifficulty(raw.difficulty());
            if (difficulty == 0) {
                errors.add("难度仅支持 简单/中等/困难");
            }

            String answer = null;
            List<QuestionImportOption> options = new ArrayList<>();
            if (type == 1 || type == 2) {
                Map<String, String> optionTexts = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : raw.options().entrySet()) {
                    String text = entry.getValue().trim();
                    if (!StringUtils.hasText(text)) {
                        continue;
                    }
                    if (text.length() > 500) {
                        errors.add("选项" + entry.getKey() + "不能超过 500 个字符");
                    } else {
                        optionTexts.put(entry.getKey(), text);
                    }
                }
                if (optionTexts.size() < 2) {
                    errors.add("单选/多选题至少需要 2 个选项");
                }
                Set<String> correctKeys = parseCorrectKeys(raw.correctAnswer(), optionTexts.keySet(), errors);
                if (type == 1 && correctKeys.size() != 1) {
                    errors.add("单选题必须且只能有 1 个正确答案");
                }
                if (type == 2 && correctKeys.size() < 1) {
                    errors.add("多选题至少需要 1 个正确答案");
                }
                for (Map.Entry<String, String> entry : optionTexts.entrySet()) {
                    options.add(new QuestionImportOption(
                            entry.getKey(), entry.getValue(), correctKeys.contains(entry.getKey())));
                }
            } else if (type == 3) {
                answer = normalizeJudgeAnswer(raw.correctAnswer());
                if (answer == null) {
                    errors.add("判断题答案必须为「正确」或「错误」");
                }
            } else if (type == 4) {
                answer = trimToNull(raw.correctAnswer());
                if (answer == null) {
                    errors.add("简答题必须填写参考答案");
                }
            }

            String tags = trimToNull(raw.tags());
            if (tags != null && tags.length() > 255) {
                errors.add("标签不能超过 255 个字符");
            }
            String analysis = trimToNull(raw.analysis());
            if (analysis != null && analysis.length() > 10000) {
                errors.add("解析过长");
            }

            if (!errors.isEmpty()) {
                errorRows.add(new QuestionImportErrorRow(raw.rowNumber(), content, errors));
                continue;
            }
            validRows.add(new QuestionImportRow(
                    raw.rowNumber(),
                    content,
                    stageName,
                    type,
                    difficulty,
                    options,
                    answer,
                    analysis,
                    tags));
        }

        String token = null;
        if (!validRows.isEmpty()) {
            token = UUID.randomUUID().toString().replace("-", "");
            try {
                QuestionImportPayload payload = new QuestionImportPayload(subject.id(), validRows);
                stringRedisTemplate.opsForValue().set(
                        PREVIEW_KEY_PREFIX + token,
                        objectMapper.writeValueAsString(payload),
                        PREVIEW_TTL);
            } catch (Exception ex) {
                throw BizException.of(ErrorCode.INTERNAL_ERROR, "导入预览暂存失败，请稍后重试");
            }
        }
        List<QuestionImportRow> previewRows = validRows.size() <= PREVIEW_LIMIT
                ? validRows
                : validRows.subList(0, PREVIEW_LIMIT);
        return new QuestionImportPreviewResponse(
                token,
                rawRows.size(),
                validRows.size(),
                errorRows.size(),
                subject.id(),
                subject.name(),
                new ArrayList<>(stagesToCreate),
                previewRows,
                errorRows);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionImportConfirmResponse confirm(String token) {
        String key = PREVIEW_KEY_PREFIX + token;
        String json = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (!StringUtils.hasText(json)) {
            throw BizException.of(ErrorCode.NOT_FOUND, "导入预览已过期，请重新上传 Excel");
        }
        QuestionImportPayload payload;
        try {
            payload = objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.NOT_FOUND, "导入预览已失效，请重新上传 Excel");
        }
        SubjectOption subject = subjectService.listOptions().stream()
                .filter(option -> option.id().equals(payload.subjectId()))
                .findFirst()
                .orElseThrow(() -> BizException.of(ErrorCode.PARAM_INVALID, "目标学科不存在"));
        if (payload.rows() == null || payload.rows().isEmpty()) {
            throw BizException.of(ErrorCode.PARAM_INVALID, "没有可导入的题目");
        }

        Map<String, Long> stageIds = new HashMap<>();
        for (Stage stage : stageMapper.selectList(Wrappers.<Stage>lambdaQuery()
                .eq(Stage::getSubjectId, subject.id()))) {
            stageIds.putIfAbsent(stage.getName().trim(), stage.getId());
        }
        int stagesCreated = 0;
        for (QuestionImportRow row : payload.rows()) {
            if (!stageIds.containsKey(row.stageName())) {
                stageIds.put(row.stageName(), subjectService.ensureStage(subject.id(), row.stageName()));
                stagesCreated++;
            }
        }

        for (QuestionImportRow row : payload.rows()) {
            Question question = new Question();
            question.setStageId(stageIds.get(row.stageName()));
            question.setType(row.type());
            question.setDifficulty(row.difficulty());
            question.setContent(row.content());
            question.setAnswer(row.answer());
            question.setAnalysis(row.analysis());
            question.setTags(row.tags());
            question.setStatus(3);
            question.setRejectReason(null);
            questionMapper.insert(question);
            for (QuestionImportOption option : row.options()) {
                QuestionOption entity = new QuestionOption();
                entity.setQuestionId(question.getId());
                entity.setOptionKey(option.key());
                entity.setOptionText(option.text());
                entity.setIsCorrect(option.correct() ? 1 : 0);
                entity.setSortOrder(option.key().charAt(0) - 'A' + 1);
                questionOptionMapper.insert(entity);
            }
        }
        return new QuestionImportConfirmResponse(payload.rows().size(), stagesCreated);
    }

    @Override
    public byte[] template() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("题目数据");
            String[] headers = {"题干", "分类", "题型", "难度", "选项A", "选项B", "选项C",
                    "选项D", "选项E", "选项F", "正确答案", "解析", "标签"};
            int[] widths = {50, 28, 10, 10, 26, 26, 26, 26, 20, 20, 16, 40, 20};
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            Row header = sheet.createRow(0);
            header.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                header.getCell(i).setCellStyle(headerStyle);
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            String[][] samples = {
                    {"下列哪种类型不是 Collection 集合（）", "AI工程化-基础与项目实战", "单选", "简单",
                            "List", "Set", "Map", "Queue", "", "", "C",
                            "Map 是 Java 集合框架中与 Collection 并列的接口", "JavaSE基础"},
                    {"下列属于 Python 算术运算符的有（）", "AI应用全栈开发基础", "多选", "简单",
                            "+", "//", "**", "and", "", "", "A,B,C", "", "python核心"},
                    {"请简述方法的概念和优势。", "AI工程化-基础与项目实战", "简答", "中等",
                            "", "", "", "", "", "", "方法是将完成独立功能的代码块组织起来，可提高复用性、可读性和可维护性。",
                            "考察方法的概念", "JavaSE基础"}
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
                    "1. 第一行表头不可删除或改名。",
                    "2. 必填：题干、分类、题型、难度、正确答案；单选/多选还需至少 2 个选项。",
                    "3. 分类将作为导入时选择的「目标学科」下的阶段；阶段不存在时自动创建。",
                    "4. 题型填写：单选 / 多选 / 判断 / 简答；难度填写：简单 / 中等 / 困难。",
                    "5. 单选答案填单个字母（如 C）；多选答案用英文逗号分隔（如 A,B,C）。",
                    "6. 判断/简答题无需填写选项，答案分别填「正确/错误」或参考答案。",
                    "7. 单次最多导入 500 道；单文件不超过 10MB。"
            };
            for (int i = 0; i < noteLines.length; i++) {
                notes.createRow(i).createCell(0).setCellValue(noteLines[i]);
            }
            notes.setColumnWidth(0, 100 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "生成题库导入模板失败，请稍后重试");
        }
    }

    private int parseType(String value) {
        String text = value == null ? "" : value.trim();
        return switch (text) {
            case "单选", "单选题", "1" -> 1;
            case "多选", "多选题", "2" -> 2;
            case "判断", "判断题", "3" -> 3;
            case "简答", "简答题", "4" -> 4;
            default -> 0;
        };
    }

    private int parseDifficulty(String value) {
        String text = value == null ? "" : value.trim();
        return switch (text) {
            case "简单", "1" -> 1;
            case "中等", "2" -> 2;
            case "困难", "3" -> 3;
            default -> 0;
        };
    }

    private Set<String> parseCorrectKeys(String value, Set<String> optionKeys, List<String> errors) {
        Set<String> result = new LinkedHashSet<>();
        String text = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(text)) {
            errors.add("缺少正确答案");
            return result;
        }
        for (String token : text.split("[,，、\\s]+")) {
            String key = token.trim();
            if (!key.isEmpty()) {
                if (!optionKeys.contains(key)) {
                    errors.add("正确答案「" + key + "」对应选项不存在");
                } else {
                    result.add(key);
                }
            }
        }
        return result;
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
