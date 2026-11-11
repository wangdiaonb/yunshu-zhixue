package com.yunshu.zhixue.modules.course.service;

import com.yunshu.zhixue.modules.course.dto.CourseImportConfirmResponse;
import com.yunshu.zhixue.modules.course.dto.CourseImportPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 课程 Excel 批量导入（A-C06 / A-C07）。
 */
public interface CourseImportService {

    CourseImportPreviewResponse preview(MultipartFile file);

    CourseImportConfirmResponse confirm(String token);

    /** 生成课程 Excel 导入模板。 */
    byte[] template();
}
