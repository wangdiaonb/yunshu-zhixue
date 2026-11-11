package com.yunshu.zhixue.modules.subject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.subject.entity.Subject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学科 Mapper，附带跨内容表的只读统计查询。
 */
@Mapper
public interface SubjectMapper extends BaseMapper<Subject> {

    /** 学科关联课程数：主学科 + 额外学科（M:N），去重。 */
    @Select("""
            SELECT COUNT(DISTINCT c.id)
            FROM course c
            LEFT JOIN course_subject cs ON cs.course_id = c.id AND cs.delete_flag = 0
            WHERE c.delete_flag = 0
              AND (c.subject_id = #{subjectId} OR cs.subject_id = #{subjectId})
            """)
    long countCourses(@Param("subjectId") Long subjectId);

    /** 学科关联学习路线数。 */
    @Select("""
            SELECT COUNT(*)
            FROM learning_route
            WHERE delete_flag = 0 AND subject_id = #{subjectId}
            """)
    long countRoutes(@Param("subjectId") Long subjectId);

    /** 学科下全部阶段的题目数。 */
    @Select("""
            SELECT COUNT(*)
            FROM question q
            INNER JOIN stage s ON s.id = q.stage_id
            WHERE q.delete_flag = 0 AND s.delete_flag = 0 AND s.subject_id = #{subjectId}
            """)
    long countQuestions(@Param("subjectId") Long subjectId);

    /** 全平台题目总数（统计卡）。 */
    @Select("SELECT COUNT(*) FROM question WHERE delete_flag = 0")
    long countAllQuestions();

    /** 已有阶段的学科数（统计卡）。 */
    @Select("SELECT COUNT(DISTINCT subject_id) FROM stage WHERE delete_flag = 0")
    long countSubjectsWithStages();
}
