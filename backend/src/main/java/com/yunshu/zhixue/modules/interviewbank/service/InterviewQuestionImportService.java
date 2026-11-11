package com.yunshu.zhixue.modules.interviewbank.service;

import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportConfirmResponse;
import com.yunshu.zhixue.modules.interviewbank.dto.InterviewQuestionImportPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * 面试题 Excel 批量导入服务（A-I07 / A-I08 / A-I16）。
 */
public interface InterviewQuestionImportService {

    /**
     * 解析预览（两段式①）。
     *
     * @param file             Excel 文件
     * @param defaultSubjectId Excel 未填写「所属学科」时使用的默认学科，可为空
     */
    InterviewQuestionImportPreviewResponse preview(MultipartFile file, Long defaultSubjectId);

    /** 确认入库（两段式②），一次性消费预览令牌。 */
    InterviewQuestionImportConfirmResponse confirm(String token);

    /** 下载导入模板。 */
    byte[] template();
}
