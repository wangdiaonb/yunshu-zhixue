package com.yunshu.zhixue.modules.question.service;

import com.yunshu.zhixue.modules.question.dto.QuestionImportConfirmResponse;
import com.yunshu.zhixue.modules.question.dto.QuestionImportPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 题库 Excel 批量导入（A-Q12 / A-Q13 / A-Q14）。
 */
public interface QuestionImportService {

    QuestionImportPreviewResponse preview(MultipartFile file, Long subjectId);

    QuestionImportConfirmResponse confirm(String token);

    byte[] template();
}
