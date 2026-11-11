package com.yunshu.zhixue.modules.course.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import com.yunshu.zhixue.modules.course.dto.CourseImportConfirmResponse;
import com.yunshu.zhixue.modules.course.dto.CourseImportErrorRow;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreviewResponse;
import com.yunshu.zhixue.modules.course.dto.CourseImportRow;
import com.yunshu.zhixue.modules.course.entity.Course;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import com.yunshu.zhixue.modules.course.mapper.CourseMapper;
import com.yunshu.zhixue.modules.course.mapper.CourseSubjectMapper;
import com.yunshu.zhixue.modules.course.service.CourseImportService;
import com.yunshu.zhixue.modules.course.support.CourseExcelParser;
import com.yunshu.zhixue.modules.course.support.RawCourseRow;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CourseImportServiceImpl implements CourseImportService {

    private static final Duration PREVIEW_TTL = Duration.ofMinutes(30);
    private static final String PREVIEW_KEY_PREFIX = "course:import:";
    private static final int PREVIEW_LIMIT = 50;
    private static final Pattern BVID_PATTERN = Pattern.compile("BV[0-9A-Za-z]{10}");
    private static final String OTHER_SUBJECT = "其他学科";

    private final CourseExcelParser excelParser;
    private final CourseMapper courseMapper;
    private final CourseSubjectMapper courseSubjectMapper;
    private final SubjectService subjectService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public CourseImportPreviewResponse preview(MultipartFile file) {
        List<RawCourseRow> rawRows = excelParser.parse(file);
        List<SubjectOption> options = subjectService.listOptions();
        Map<String, SubjectOption> subjectMap = new HashMap<>();
        for (SubjectOption option : options) {
            subjectMap.putIfAbsent(option.name().trim(), option);
        }

        Set<String> existingBvids = courseMapper.selectList(Wrappers.<Course>lambdaQuery()
                        .select(Course::getBvid)
                        .isNotNull(Course::getBvid))
                .stream()
                .map(Course::getBvid)
                .filter(StringUtils::hasText)
                .map(this::key)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> existingNames = courseMapper.selectList(Wrappers.<Course>lambdaQuery()
                        .select(Course::getName))
                .stream()
                .map(Course::getName)
                .filter(StringUtils::hasText)
                .map(this::key)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> fileBvids = new HashSet<>();
        Set<String> fileNames = new HashSet<>();
        List<CourseImportRow> validRows = new ArrayList<>();
        List<CourseImportErrorRow> errorRows = new ArrayList<>();

        for (RawCourseRow raw : rawRows) {
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();

            String name = raw.name().trim();
            if (!StringUtils.hasText(name)) {
                errors.add("缺少课程名称");
            } else if (name.length() > 100) {
                errors.add("课程名称不能超过 100 个字符");
            }
            if (StringUtils.hasText(name)) {
                if (existingNames.contains(key(name)) || !fileNames.add(key(name))) {
                    warnings.add("已存在同名课程");
                }
            }

            List<SubjectOption> matchedSubjects = new ArrayList<>();
            String subjectText = raw.subjects();
            if (StringUtils.hasText(subjectText)) {
                for (String token : subjectText.split("[,，、]")) {
                    String subjectName = token.trim();
                    if (!StringUtils.hasText(subjectName) || subjectName.startsWith(OTHER_SUBJECT)) {
                        continue;
                    }
                    SubjectOption option = findSubject(subjectName, subjectMap, options);
                    if (option == null) {
                        errors.add("未识别学科「" + subjectName + "」");
                    } else if (matchedSubjects.stream().noneMatch(item -> item.id().equals(option.id()))) {
                        matchedSubjects.add(option);
                    }
                }
            }
            Long subjectId = matchedSubjects.isEmpty() ? null : matchedSubjects.get(0).id();
            List<Long> extraSubjectIds = matchedSubjects.stream()
                    .skip(1)
                    .map(SubjectOption::id)
                    .toList();
            List<String> subjectNames = matchedSubjects.isEmpty()
                    ? List.of(OTHER_SUBJECT)
                    : matchedSubjects.stream().map(SubjectOption::name).toList();

            int source;
            String sourceText = raw.source().trim();
            if (!StringUtils.hasText(sourceText)
                    || "B站".equalsIgnoreCase(sourceText)
                    || "bilibili".equalsIgnoreCase(sourceText)
                    || "1".equals(sourceText)) {
                source = 1;
            } else if ("其他".equals(sourceText)
                    || "other".equalsIgnoreCase(sourceText)
                    || "2".equals(sourceText)) {
                source = 2;
            } else {
                source = 1;
                errors.add("来源仅支持 B站 或 其他");
            }

            int durationMinutes = 0;
            String durationText = raw.duration().replace(",", "").trim();
            try {
                durationMinutes = new BigDecimal(durationText).intValue();
                if (durationMinutes <= 0 || durationMinutes > 100000) {
                    errors.add("时长必须大于 0 且不超过 100000 分钟");
                }
            } catch (NumberFormatException ex) {
                errors.add("时长格式不正确");
            }

            String link = trimToNull(raw.link());
            String coverUrl = trimToNull(raw.coverUrl());
            if (link != null && link.length() > 500) {
                errors.add("课程链接不能超过 500 个字符");
            }
            if (coverUrl != null && coverUrl.length() > 500) {
                errors.add("封面地址不能超过 500 个字符");
            }

            String bvid = trimToNull(raw.bvid());
            if (!StringUtils.hasText(bvid)) {
                bvid = extractBvid(link);
            }
            if (StringUtils.hasText(bvid)) {
                if (bvid.length() > 20) {
                    errors.add("BV 号不能超过 20 个字符");
                } else if (!BVID_PATTERN.matcher(bvid).matches()) {
                    errors.add("BV 号格式不正确");
                } else {
                    String bvidKey = key(bvid);
                    if (!fileBvids.add(bvidKey)) {
                        errors.add("文件内 BV 号重复");
                    } else if (existingBvids.contains(bvidKey)) {
                        errors.add("BV 号已存在");
                    }
                }
            } else if (source == 1) {
                warnings.add("未识别 BV 号");
            }

            if (!errors.isEmpty()) {
                errorRows.add(new CourseImportErrorRow(raw.rowNumber(), name, errors));
                continue;
            }
            validRows.add(new CourseImportRow(
                    raw.rowNumber(),
                    name,
                    subjectId,
                    extraSubjectIds,
                    subjectNames,
                    durationMinutes,
                    source,
                    bvid,
                    link,
                    coverUrl,
                    warnings));
        }

        String token = null;
        if (!validRows.isEmpty()) {
            token = UUID.randomUUID().toString().replace("-", "");
            try {
                stringRedisTemplate.opsForValue().set(
                        PREVIEW_KEY_PREFIX + token,
                        objectMapper.writeValueAsString(validRows),
                        PREVIEW_TTL);
            } catch (Exception ex) {
                throw BizException.of(ErrorCode.INTERNAL_ERROR, "导入预览暂存失败，请稍后重试");
            }
        }

        List<CourseImportRow> previewRows = validRows.size() <= PREVIEW_LIMIT
                ? validRows
                : validRows.subList(0, PREVIEW_LIMIT);
        return new CourseImportPreviewResponse(
                token,
                rawRows.size(),
                validRows.size(),
                errorRows.size(),
                previewRows,
                errorRows);
    }

    @Override
    public byte[] template() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("课程导入");
            String[] headers = {"课程名称", "所属学科", "来源", "时长（分钟）", "BV号", "封面", "播放链接"};
            int[] widths = {42, 32, 10, 14, 16, 48, 52};

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            String[][] samples = {
                    {"Java零基础视频教程", "AI应用与智能体开发", "B站", "4512",
                            "BV17F411T7Ao", "",
                            "https://www.bilibili.com/video/BV17F411T7Ao"},
                    {"跨学科实战课程示例", "AI测试,AI大模型开发", "B站", "120",
                            "BV1Demo12345", "",
                            "https://www.bilibili.com/video/BV1Demo12345"},
                    {"其他来源课程示例", "其他学科（原前端-鸿蒙-安卓-大数据等）", "其他", "90",
                            "", "", "https://example.com/course"}
            };
            for (int rowIndex = 0; rowIndex < samples.length; rowIndex++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIndex + 1);
                for (int column = 0; column < samples[rowIndex].length; column++) {
                    String value = samples[rowIndex][column];
                    if (column == 3 && StringUtils.hasText(value)) {
                        row.createCell(column).setCellValue(Double.parseDouble(value));
                    } else {
                        row.createCell(column).setCellValue(value);
                    }
                }
            }
            sheet.createFreezePane(0, 1);

            org.apache.poi.ss.usermodel.Sheet notes = workbook.createSheet("填写说明");
            String[] noteLines = {
                    "1. 第一行表头不可删除或改名。",
                    "2. 课程名称、所属学科、时长（分钟）为必填项。",
                    "3. 所属学科可用英文逗号填写多个，第一个作为主学科，其余作为额外关联学科。",
                    "4. 其他学科可填写「其他学科」或「其他学科（原前端-鸿蒙-安卓-大数据等）」。",
                    "5. 来源填写「B站」或「其他」，留空默认为 B站。",
                    "6. 时长单位为分钟；封面、播放链接可留空；单次最多导入 1000 条。"
            };
            for (int i = 0; i < noteLines.length; i++) {
                notes.createRow(i).createCell(0).setCellValue(noteLines[i]);
            }
            notes.setColumnWidth(0, 100 * 256);
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "生成导入模板失败，请稍后重试");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseImportConfirmResponse confirm(String token) {
        String key = PREVIEW_KEY_PREFIX + token;
        String json = stringRedisTemplate.opsForValue().getAndDelete(key);
        if (!StringUtils.hasText(json)) {
            throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "预览已过期，请重新上传 Excel");
        }
        List<CourseImportRow> rows;
        try {
            rows = objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "预览数据已失效，请重新上传 Excel");
        }
        if (rows == null || rows.isEmpty()) {
            throw BizException.of(ErrorCode.IMPORT_VALIDATION_FAILED, "没有可导入的课程");
        }

        try {
            for (CourseImportRow row : rows) {
                Course course = new Course();
                course.setName(row.name());
                course.setCoverUrl(row.coverUrl());
                course.setDurationMinutes(row.durationMinutes());
                course.setSource(row.source());
                course.setLink(row.link());
                course.setBvid(row.bvid());
                course.setSubjectId(row.subjectId());
                courseMapper.insert(course);
                for (Long subjectId : row.subjectIds()) {
                    CourseSubject relation = new CourseSubject();
                    relation.setCourseId(course.getId());
                    relation.setSubjectId(subjectId);
                    courseSubjectMapper.insert(relation);
                }
            }
        } catch (DuplicateKeyException ex) {
            throw BizException.of(ErrorCode.DUPLICATE_NAME, "存在重复的 BV 号，请重新预览");
        }
        return new CourseImportConfirmResponse(rows.size());
    }

    private SubjectOption findSubject(String token, Map<String, SubjectOption> subjectMap,
                                      List<SubjectOption> options) {
        SubjectOption exact = subjectMap.get(token);
        if (exact != null) {
            return exact;
        }
        for (SubjectOption option : options) {
            if (token.contains(option.name()) || option.name().contains(token)) {
                return option;
            }
        }
        return null;
    }

    private String extractBvid(String link) {
        if (!StringUtils.hasText(link)) {
            return null;
        }
        Matcher matcher = BVID_PATTERN.matcher(link);
        return matcher.find() ? matcher.group() : null;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
