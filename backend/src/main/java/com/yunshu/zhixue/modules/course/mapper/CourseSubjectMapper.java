package com.yunshu.zhixue.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.course.entity.CourseSubject;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 课程-学科关联 Mapper。
 */
@Mapper
public interface CourseSubjectMapper extends BaseMapper<CourseSubject> {

    /**
     * 物理删除课程的额外学科关联。
     * 唯一键 (course_id, subject_id) 不含 delete_flag，逻辑删除后无法重建同一关联。
     */
    @Delete("DELETE FROM course_subject WHERE course_id = #{courseId}")
    int deletePhysicalByCourseId(@Param("courseId") Long courseId);
}
