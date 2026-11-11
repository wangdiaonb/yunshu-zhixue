package com.yunshu.zhixue.modules.course.controller.admin;

import com.yunshu.zhixue.common.constant.AppConstants;
import com.yunshu.zhixue.common.response.ApiResponse;
import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.course.dto.CourseImportConfirmRequest;
import com.yunshu.zhixue.modules.course.dto.CourseImportConfirmResponse;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreviewResponse;
import com.yunshu.zhixue.modules.course.dto.CourseItem;
import com.yunshu.zhixue.modules.course.dto.CourseQuery;
import com.yunshu.zhixue.modules.course.dto.CourseSaveRequest;
import com.yunshu.zhixue.modules.course.dto.CourseStatsResponse;
import com.yunshu.zhixue.modules.course.service.CourseImportService;
import com.yunshu.zhixue.modules.course.service.CourseService;
import com.yunshu.zhixue.security.RequirePermission;
import com.yunshu.zhixue.support.log.OperationAction;
import com.yunshu.zhixue.support.log.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端 B 站课程库接口（A-C01 ~ A-C07）。
 */
@RestController
@RequestMapping(AppConstants.ADMIN_API_PREFIX + "/courses")
@RequiredArgsConstructor
@Tag(name = "管理端-课程库")
public class CourseAdminController {

    private final CourseService courseService;
    private final CourseImportService courseImportService;

    @GetMapping("/stats")
    @RequirePermission("course:view")
    @Operation(summary = "课程库统计卡")
    public ApiResponse<CourseStatsResponse> stats() {
        return ApiResponse.ok(courseService.stats());
    }

    @GetMapping
    @RequirePermission("course:view")
    @Operation(summary = "课程分页列表")
    public ApiResponse<PageResult<CourseItem>> page(@Valid CourseQuery query) {
        return ApiResponse.ok(courseService.pageCourses(query));
    }

    @PostMapping
    @RequirePermission("course:add")
    @OperationLog(action = OperationAction.CREATE, targetType = "course", targetName = "#request.name")
    @Operation(summary = "新增课程")
    public ApiResponse<Long> create(@Valid @RequestBody CourseSaveRequest request) {
        return ApiResponse.ok(courseService.createCourse(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("course:edit")
    @OperationLog(action = OperationAction.UPDATE, targetType = "course", targetName = "#request.name")
    @Operation(summary = "编辑课程")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @Valid @RequestBody CourseSaveRequest request) {
        courseService.updateCourse(id, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("course:delete")
    @OperationLog(action = OperationAction.DELETE, targetType = "course", targetName = "#id")
    @Operation(summary = "删除课程")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ApiResponse.ok();
    }

    @GetMapping("/import/template")
    @RequirePermission("course:add")
    @Operation(summary = "下载课程 Excel 导入模板")
    public ResponseEntity<byte[]> importTemplate() {
        byte[] bytes = courseImportService.template();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("课程导入模板.xlsx", java.nio.charset.StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequirePermission("course:add")
    @Operation(summary = "课程 Excel 导入预览")
    public ApiResponse<CourseImportPreviewResponse> previewImport(
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(courseImportService.preview(file));
    }

    @PostMapping("/import/confirm")
    @RequirePermission("course:add")
    @OperationLog(action = OperationAction.CREATE, targetType = "course", targetName = "课程批量导入")
    @Operation(summary = "确认课程 Excel 导入")
    public ApiResponse<CourseImportConfirmResponse> confirmImport(
            @Valid @RequestBody CourseImportConfirmRequest request) {
        return ApiResponse.ok(courseImportService.confirm(request.token()));
    }
}
