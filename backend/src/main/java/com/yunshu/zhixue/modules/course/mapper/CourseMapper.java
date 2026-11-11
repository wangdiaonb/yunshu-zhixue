package com.yunshu.zhixue.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 课程 Mapper，附带课程库统计与删除保护查询。
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    @Select("SELECT COALESCE(SUM(duration_minutes), 0) FROM course WHERE delete_flag = 0")
    long sumDurationMinutes();

    /** 关联学科数：主学科 + 额外学科，排除空主学科（其他学科）。 */
    @Select("""
            SELECT COUNT(DISTINCT subject_id)
            FROM (
                SELECT subject_id
                FROM course
                WHERE delete_flag = 0 AND subject_id IS NOT NULL
                UNION
                SELECT cs.subject_id
                FROM course_subject cs
                INNER JOIN course c ON c.id = cs.course_id AND c.delete_flag = 0
                WHERE cs.delete_flag = 0
            ) related
            """)
    long countRelatedSubjects();

    /** 课程被学习路线引用的次数（删除保护）。 */
    @Select("SELECT COUNT(*) FROM route_course WHERE delete_flag = 0 AND course_id = #{courseId}")
    long countRouteReferences(@Param("courseId") Long courseId);
}
