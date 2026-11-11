package com.yunshu.zhixue.modules.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增/编辑课程（A-C03 / A-C04）。
 */
public record CourseSaveRequest(

        @NotBlank(message = "请输入课程名称")
        @Size(max = 100, message = "课程名称不能超过 100 个字符")
        String name,

        @Size(max = 500, message = "封面地址不能超过 500 个字符")
        String coverUrl,

        @NotNull(message = "请输入课程时长")
        @Min(value = 1, message = "课程时长必须大于 0")
        @Max(value = 100000, message = "课程时长过大")
        Integer durationMinutes,

        @NotNull(message = "请选择课程来源")
        @Min(value = 1, message = "来源参数不合法")
        @Max(value = 2, message = "来源参数不合法")
        Integer source,

        @Size(max = 500, message = "课程链接不能超过 500 个字符")
        String link,

        @Size(max = 20, message = "BV 号不能超过 20 个字符")
        String bvid,

        /** 主学科；null 或 0 表示其他学科。 */
        @Min(value = 0, message = "学科参数不合法")
        Long subjectId,

        /** 额外关联学科（不包含主学科）。 */
        @Size(max = 20, message = "关联学科过多")
        List<Long> subjectIds
) {
}
