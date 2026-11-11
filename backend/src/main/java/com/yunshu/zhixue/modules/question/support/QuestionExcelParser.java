package com.yunshu.zhixue.modules.question.support;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 题库 Excel 解析器。
 *
 * <p>兼容两种表头：题库模板的「题干/分类/题型…」，以及设计稿的「题目/阶段/类型…」。
 * 支持选项 A~F，单选/多选/判断/简答。</p>
 */
@Component
public class QuestionExcelParser {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_DATA_ROWS = 500;

    private static final String COL_CONTENT = "题干";
    private static final String COL_CATEGORY = "分类";
    private static final String COL_TYPE = "题型";
    private static final String COL_DIFFICULTY = "难度";
    private static final String COL_ANSWER = "正确答案";
    private static final String COL_ANALYSIS = "解析";
    private static final String COL_TAGS = "标签";

    private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

    public List<RawQuestionRow> parse(MultipartFile file) {
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
            List<RawQuestionRow> rows = new ArrayList<>();
            for (int index = layout.headerRowIndex() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                Map<String, String> options = new LinkedHashMap<>();
                for (int i = 0; i < layout.optionColumns().length; i++) {
                    options.put(String.valueOf((char) ('A' + i)), cell(row, layout.optionColumns()[i]));
                }
                RawQuestionRow raw = new RawQuestionRow(
                        index + 1,
                        cell(row, layout.contentColumn()),
                        cell(row, layout.categoryColumn()),
                        cell(row, layout.typeColumn()),
                        cell(row, layout.difficultyColumn()),
                        options,
                        cell(row, layout.answerColumn()),
                        cell(row, layout.analysisColumn()),
                        cell(row, layout.tagsColumn()));
                if (!hasValue(raw)) {
                    continue;
                }
                rows.add(raw);
                if (rows.size() > MAX_DATA_ROWS) {
                    throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED,
                            "单次最多导入 " + MAX_DATA_ROWS + " 道题目");
                }
            }
            if (rows.isEmpty()) {
                throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "Excel 中没有可导入的题目数据");
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
            Integer content = findColumn(columns, COL_CONTENT, "题目");
            Integer category = findColumn(columns, COL_CATEGORY, "阶段");
            Integer type = findColumn(columns, COL_TYPE, "类型");
            Integer difficulty = findColumn(columns, COL_DIFFICULTY);
            Integer answer = findColumn(columns, COL_ANSWER, "答案");
            if (content != null && category != null && type != null && difficulty != null && answer != null) {
                Integer[] optionColumns = new Integer[6];
                for (int i = 0; i < 6; i++) {
                    optionColumns[i] = findColumn(columns, "选项" + (char) ('A' + i));
                }
                return new HeaderLayout(
                        index,
                        content,
                        category,
                        type,
                        difficulty,
                        optionColumns,
                        answer,
                        findColumn(columns, COL_ANALYSIS),
                        findColumn(columns, COL_TAGS));
            }
        }
        throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED,
                "未找到表头，请使用包含「题干、分类、题型、难度、正确答案」的模板");
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

    private boolean hasValue(RawQuestionRow row) {
        if (StringUtils.hasText(row.content()) || StringUtils.hasText(row.category())
                || StringUtils.hasText(row.type()) || StringUtils.hasText(row.difficulty())
                || StringUtils.hasText(row.correctAnswer()) || StringUtils.hasText(row.analysis())
                || StringUtils.hasText(row.tags())) {
            return true;
        }
        return row.options().values().stream().anyMatch(StringUtils::hasText);
    }

    private record HeaderLayout(int headerRowIndex, Integer contentColumn, Integer categoryColumn,
                                Integer typeColumn, Integer difficultyColumn, Integer[] optionColumns,
                                Integer answerColumn, Integer analysisColumn, Integer tagsColumn) {
    }
}
