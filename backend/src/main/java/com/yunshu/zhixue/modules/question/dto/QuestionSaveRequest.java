package com.yunshu.zhixue.modules.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增/编辑题目（A-Q04 / A-Q05）。
 */
public record QuestionSaveRequest(

        @NotBlank(message = "请输入题干")
        @Size(max = 10000, message = "题干过长")
        String content,

        @NotNull(message = "请选择阶段")
        @Min(value = 1, message = "阶段参数不合法")
        Long stageId,

        @NotNull(message = "请选择题型")
        @Min(value = 1, message = "题型参数不合法")
        @Max(value = 4, message = "题型参数不合法")
        Integer type,

        @NotNull(message = "请选择难度")
        @Min(value = 1, message = "难度参数不合法")
        @Max(value = 3, message = "难度参数不合法")
        Integer difficulty,

        @Size(max = 255, message = "标签不能超过 255 个字符")
        String tags,

        @Size(max = 10000, message = "解析过长")
        String analysis,

        @Size(max = 10000, message = "答案过长")
        String answer,

        @Size(max = 6, message = "选项最多 6 个")
        List<@Valid QuestionOptionSave> options
) {
}
