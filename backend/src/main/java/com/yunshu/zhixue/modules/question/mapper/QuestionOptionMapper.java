package com.yunshu.zhixue.modules.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.question.entity.QuestionOption;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 题目选项 Mapper。
 */
@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOption> {

    /**
     * 物理删除选项：唯一键 (question_id, option_key) 不含 delete_flag，
     * 编辑时需先物理清理再按新选项重建。
     */
    @Delete("DELETE FROM question_option WHERE question_id = #{questionId}")
    int deletePhysicalByQuestion(@Param("questionId") Long questionId);
}
