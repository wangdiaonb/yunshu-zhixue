package com.yunshu.zhixue.modules.route.mapper;

import com.yunshu.zhixue.modules.route.dto.RouteCompletionStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 学员课程完成表 Mapper。
 *
 * <p>当前仅提供跨模块只读的完成数聚合（学员管理「已学课程」），
 * 完成勾选业务写入待 route 模块开发时补齐。</p>
 */
@Mapper
public interface RouteCourseCompletionMapper {

    /** 按学员批量聚合已学课程数。 */
    @Select("""
            <script>
            SELECT student_id AS studentId, COUNT(*) AS courseCount
            FROM route_course_completion
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="studentIds" item="studentId" open="(" separator="," close=")">
                #{studentId}
              </foreach>
            GROUP BY student_id
            </script>
            """)
    List<RouteCompletionStat> countByStudents(@Param("studentIds") Collection<Long> studentIds);
}
