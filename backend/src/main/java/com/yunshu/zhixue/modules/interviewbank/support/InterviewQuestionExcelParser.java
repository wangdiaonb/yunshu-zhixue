package com.yunshu.zhixue.modules.interviewbank.support;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 面试题 Excel 解析器。
 *
 * <p>模板列：所属学科 / 题干 / 难度 / 主题 / 标签 / 参考答案；表头别名兼容
 * 「学科 / 方向」「题目 / 问题」「分类」「答案」。仅「题干」为必需列，
 * 其余列缺失时按默认值处理（所属学科由导入时选择的默认学科兜底）。</p>
 */
@Component
public class InterviewQuestionExcelParser {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final String COL_SUBJECT = "所属学科";
    private static final String COL_CONTENT = "题干";
    private static final String COL_DIFFICULTY = "难度";
    private static final String COL_TOPIC = "主题";
    private static final String COL_TAGS = "标签";
    private static final String COL_ANSWER = "参考答案";

    private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

    public List<RawInterviewQuestionRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "请选择要导入的 Excel 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "Excel 文件不能超过 10MB");
        }
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lower = filename.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
                throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "仅支持 .xlsx / .xls 文件");
            }
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "Excel 中没有工作表");
            }
            Sheet sheet = workbook.getSheetAt(0);
            HeaderLayout layout = findHeader(sheet);
            List<RawInterviewQuestionRow> rows = new ArrayList<>();
            for (int index = layout.headerRowIndex() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                RawInterviewQuestionRow raw = new RawInterviewQuestionRow(
                        index + 1,
                        cell(row, layout.subjectColumn()),
                        cell(row, layout.contentColumn()),
                        cell(row, layout.difficultyColumn()),
                        cell(row, layout.topicColumn()),
                        cell(row, layout.tagsColumn()),
                        cell(row, layout.answerColumn()));
                if (!hasValue(raw)) {
                    continue;
                }
                rows.add(raw);
                if (rows.size() > AppConstants.IMPORT_MAX_ROWS) {
                    throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED,
                            "单次最多导入 " + AppConstants.IMPORT_MAX_ROWS + " 道面试题");
                }
            }
            if (rows.isEmpty()) {
                throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "Excel 中没有可导入的面试题数据");
            }
            return rows;
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED,
                    "Excel 解析失败，请确认文件未加密且格式正确");
        }
    }

    private HeaderLayout findHeader(Sheet sheet) {
        int maxScan = Math.min(sheet.getLastRowNum(), 10);
        for (int index = 0; index <= maxScan; index++) {
            Row row = sheet.getRow(index);
            if (row == null) {
                continue;
            }
            Map<String, Integer> columns = new HashMap<>();
            for (int col = 0; col < row.getLastCellNum(); col++) {
                String header = normalizeHeader(cell(row, col));
                if (StringUtils.hasText(header)) {
                    columns.putIfAbsent(header, col);
                }
            }
            Integer content = findColumn(columns, COL_CONTENT, "题目", "问题");
            if (content != null) {
                return new HeaderLayout(
                        index,
                        content,
                        findColumn(columns, COL_SUBJECT, "学科", "方向", "知识库"),
                        findColumn(columns, COL_DIFFICULTY),
                        findColumn(columns, COL_TOPIC, "分类"),
                        findColumn(columns, COL_TAGS),
                        findColumn(columns, COL_ANSWER, "答案"));
            }
        }
        throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED,
                "未找到表头，请使用包含「所属学科、题干、难度、主题、标签、参考答案」的模板");
    }

    private Integer findColumn(Map<String, Integer> columns, String... aliases) {
        for (String alias : aliases) {
            Integer index = columns.get(normalizeHeader(alias));
            if (index != null) {
                return index;
            }
        }
        return null;
    }

    private String cell(Row row, Integer column) {
        if (row == null || column == null) {
            return "";
        }
        Cell cell = row.getCell(column);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\uFEFF", "")
                .replace('（', '(')
                .replace('）', ')')
                .replaceAll("\\s+", "")
                .trim();
    }

    private boolean hasValue(RawInterviewQuestionRow row) {
        return StringUtils.hasText(row.content())
                || StringUtils.hasText(row.subjectName())
                || StringUtils.hasText(row.difficulty())
                || StringUtils.hasText(row.topic())
                || StringUtils.hasText(row.tags())
                || StringUtils.hasText(row.referenceAnswer());
    }

    private record HeaderLayout(int headerRowIndex, Integer contentColumn, Integer subjectColumn,
                                Integer difficultyColumn, Integer topicColumn, Integer tagsColumn,
                                Integer answerColumn) {
    }
}
