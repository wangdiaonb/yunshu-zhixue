package com.yunshu.zhixue.modules.study.mapper;

import com.yunshu.zhixue.modules.study.dto.PracticeStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 刷题作答明细 Mapper。
 *
 * <p>当前仅提供跨模块只读的刷题聚合（学员管理/统计使用），刷题业务写入待 study 模块开发时补齐。</p>
 */
@Mapper
public interface PracticeRecordMapper {

    /**
     * 按学员批量聚合刷题数据，避免列表页逐行查询。
     *
     * <p>学习天数按业务时区（Asia/Shanghai）的自然日去重统计。</p>
     */
    @Select("""
            <script>
            SELECT student_id AS studentId,
                   COUNT(*) AS practiceCount,
                   COUNT(CASE WHEN is_correct = 1 THEN 1 END) AS correctCount,
                   COUNT(DISTINCT DATE(CONVERT_TZ(practiced_at, '+00:00', '+08:00'))) AS studyDays
            FROM practice_record
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="studentIds" item="studentId" open="(" separator="," close=")">
                #{studentId}
              </foreach>
            GROUP BY student_id
            </script>
            """)
    List<PracticeStat> statByStudents(@Param("studentIds") Collection<Long> studentIds);
}
