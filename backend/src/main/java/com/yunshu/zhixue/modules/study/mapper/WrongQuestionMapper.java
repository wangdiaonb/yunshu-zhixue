package com.yunshu.zhixue.modules.study.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 错题本只读统计（wrong_question 归属 study 模块）。
 */
@Mapper
public interface WrongQuestionMapper {

    @Select("SELECT COUNT(*) FROM wrong_question WHERE student_id = #{studentId} AND delete_flag = 0")
    long countByStudentId(@Param("studentId") Long studentId);
}
