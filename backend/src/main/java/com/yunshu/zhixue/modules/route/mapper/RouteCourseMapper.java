package com.yunshu.zhixue.modules.route.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.route.entity.RouteCourse;
import com.yunshu.zhixue.modules.route.support.RouteCourseRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 路线课程关联 Mapper，包含课程联表查询。
 */
@Mapper
public interface RouteCourseMapper extends BaseMapper<RouteCourse> {

    @Select("SELECT COUNT(*) FROM route_course WHERE delete_flag = 0 AND route_id = #{routeId}")
    int countByRoute(@Param("routeId") Long routeId);

    @Select("""
            SELECT COALESCE(SUM(c.duration_minutes), 0)
            FROM route_course rc
            INNER JOIN course c ON c.id = rc.course_id AND c.delete_flag = 0
            WHERE rc.delete_flag = 0 AND rc.route_id = #{routeId}
            """)
    long sumDurationByRoute(@Param("routeId") Long routeId);

    @Select("""
            SELECT rc.id AS routeCourseId,
                   rc.course_id AS courseId,
                   c.name,
                   c.cover_url AS coverUrl,
                   c.bvid,
                   c.duration_minutes AS durationMinutes,
                   rc.sort_order AS sortOrder
            FROM route_course rc
            INNER JOIN course c ON c.id = rc.course_id AND c.delete_flag = 0
            WHERE rc.delete_flag = 0 AND rc.route_id = #{routeId}
            ORDER BY rc.sort_order, rc.id
            LIMIT #{limit}
            """)
    List<RouteCourseRow> selectPreviewRows(@Param("routeId") Long routeId, @Param("limit") int limit);

    @Select("""
            SELECT rc.id AS routeCourseId,
                   rc.course_id AS courseId,
                   c.name,
                   c.bvid,
                   c.cover_url AS coverUrl,
                   c.duration_minutes AS durationMinutes,
                   c.source,
                   rc.sort_order AS sortOrder,
                   c.subject_id AS subjectId,
                   CASE
                       WHEN c.subject_id = #{subjectId}
                            OR EXISTS (
                                SELECT 1 FROM course_subject cs
                                WHERE cs.course_id = c.id
                                  AND cs.subject_id = #{subjectId}
                                  AND cs.delete_flag = 0
                            )
                       THEN 1 ELSE 0
                   END AS localSubject
            FROM route_course rc
            INNER JOIN course c ON c.id = rc.course_id AND c.delete_flag = 0
            WHERE rc.delete_flag = 0 AND rc.route_id = #{routeId}
            ORDER BY rc.sort_order, rc.id
            """)
    List<RouteCourseRow> selectCourseRows(@Param("routeId") Long routeId,
                                          @Param("subjectId") Long subjectId);

    @Select("""
            <script>
            SELECT c.id AS courseId,
                   c.name,
                   c.bvid,
                   c.cover_url AS coverUrl,
                   c.duration_minutes AS durationMinutes,
                   c.source,
                   c.subject_id AS subjectId,
                   CASE
                       WHEN c.subject_id = #{subjectId}
                            OR EXISTS (
                                SELECT 1 FROM course_subject cs
                                WHERE cs.course_id = c.id
                                  AND cs.subject_id = #{subjectId}
                                  AND cs.delete_flag = 0
                            )
                       THEN 1 ELSE 0
                   END AS localSubject
            FROM course c
            WHERE c.delete_flag = 0
              AND NOT EXISTS (
                  SELECT 1 FROM route_course rc
                  WHERE rc.route_id = #{routeId}
                    AND rc.course_id = c.id
                    AND rc.delete_flag = 0
              )
              <if test="keyword != null and keyword != ''">
                AND (c.name LIKE CONCAT('%', #{keyword}, '%')
                     OR c.bvid LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY localSubject DESC, c.id DESC
            </script>
            """)
    List<RouteCourseRow> selectAvailableCourses(@Param("routeId") Long routeId,
                                                @Param("subjectId") Long subjectId,
                                                @Param("keyword") String keyword);

    @Select("""
            SELECT course_id FROM route_course
            WHERE delete_flag = 0 AND route_id = #{routeId}
            """)
    List<Long> selectCourseIds(@Param("routeId") Long routeId);

    @Select("""
            SELECT id FROM route_course
            WHERE delete_flag = 0 AND route_id = #{routeId}
            ORDER BY sort_order, id
            """)
    List<Long> selectRouteCourseIds(@Param("routeId") Long routeId);

    @Select("""
            SELECT COALESCE(MAX(sort_order), 0) FROM route_course
            WHERE delete_flag = 0 AND route_id = #{routeId}
            """)
    int maxSortOrder(@Param("routeId") Long routeId);

    @Delete("DELETE FROM route_course WHERE route_id = #{routeId} AND course_id = #{courseId}")
    int deletePhysicalByCourse(@Param("routeId") Long routeId, @Param("courseId") Long courseId);

    @Delete("DELETE FROM route_course WHERE route_id = #{routeId}")
    int deletePhysicalByRoute(@Param("routeId") Long routeId);
}
