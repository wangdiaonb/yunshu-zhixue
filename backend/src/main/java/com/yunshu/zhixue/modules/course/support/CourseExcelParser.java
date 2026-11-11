package com.yunshu.zhixue.modules.course.support;

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
 * 课程 Excel 导入解析器。
 *
 * <p>支持模板列：课程名称 / 所属学科 / 来源 / 时长（分钟） / BV号 / 封面 / 播放链接。
 * 只负责读取原始值，不处理学科匹配等业务规则。</p>
 */
@Component
public class CourseExcelParser {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_DATA_ROWS = 1000;

    private static final String COL_NAME = "课程名称";
    private static final String COL_SUBJECT = "所属学科";
    private static final String COL_SOURCE = "来源";
    private static final String COL_DURATION = "时长(分钟)";
    private static final String COL_BVID = "BV号";
    private static final String COL_COVER = "封面";
    private static final String COL_LINK = "播放链接";

    private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

    public List<RawCourseRow> parse(MultipartFile file) {
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
            List<RawCourseRow> rows = new ArrayList<>();
            for (int index = layout.headerRowIndex() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null) {
                    continue;
                }
                RawCourseRow raw = new RawCourseRow(
                        index + 1,
                        cell(row, layout.nameColumn()),
                        cell(row, layout.subjectColumn()),
                        cell(row, layout.sourceColumn()),
                        cell(row, layout.durationColumn()),
                        cell(row, layout.bvidColumn()),
                        cell(row, layout.coverColumn()),
                        cell(row, layout.linkColumn()));
                if (!hasValue(raw)) {
                    continue;
                }
                rows.add(raw);
                if (rows.size() > MAX_DATA_ROWS) {
                    throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED,
                            "单次最多导入 " + MAX_DATA_ROWS + " 条课程");
                }
            }
            if (rows.isEmpty()) {
                throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "Excel 中没有可导入的课程数据");
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
            Integer name = findColumn(columns, COL_NAME);
            Integer subject = findColumn(columns, COL_SUBJECT);
            Integer duration = findColumn(columns, COL_DURATION, "时长");
            if (name != null && subject != null && duration != null) {
                return new HeaderLayout(
                        index,
                        name,
                        subject,
                        findColumn(columns, COL_SOURCE),
                        duration,
                        findColumn(columns, COL_BVID, "bvid", "BV"),
                        findColumn(columns, COL_COVER, "封面URL"),
                        findColumn(columns, COL_LINK, "课程链接", "链接"));
            }
        }
        throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED,
                "未找到表头，请使用包含「课程名称、所属学科、时长（分钟）」的模板");
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

    private boolean hasValue(RawCourseRow row) {
        return StringUtils.hasText(row.name())
                || StringUtils.hasText(row.subjects())
                || StringUtils.hasText(row.source())
                || StringUtils.hasText(row.duration())
                || StringUtils.hasText(row.bvid())
                || StringUtils.hasText(row.coverUrl())
                || StringUtils.hasText(row.link());
    }

    private record HeaderLayout(int headerRowIndex, Integer nameColumn, Integer subjectColumn,
                                Integer sourceColumn, Integer durationColumn, Integer bvidColumn,
                                Integer coverColumn, Integer linkColumn) {
    }
}
