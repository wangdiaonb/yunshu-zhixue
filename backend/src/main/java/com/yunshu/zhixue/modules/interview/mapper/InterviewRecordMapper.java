package com.yunshu.zhixue.modules.interview.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.yunshu.zhixue.modules.interview.dto.InterviewRecordItem;
import com.yunshu.zhixue.modules.interview.dto.InterviewStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 面试记录 Mapper。
 *
 * <p>当前仅提供管理端学员详情的只读查询（面试次数 / 面试记录分页），
 * 面试会话落库待 interview 模块开发时补齐。</p>
 */
@Mapper
public interface InterviewRecordMapper {

    /** 按学员批量聚合面试次数。 */
    @Select("""
            <script>
            SELECT student_id AS studentId, COUNT(*) AS interviewCount
            FROM interview_record
            WHERE delete_flag = 0
              AND student_id IN
              <foreach collection="studentIds" item="studentId" open="(" separator="," close=")">
                #{studentId}
              </foreach>
            GROUP BY student_id
            </script>
            """)
    List<InterviewStat> countByStudents(@Param("studentIds") Collection<Long> studentIds);

    /** 学员面试记录分页（按完成时间倒序）。 */
    @Select("""
            SELECT id,
                   subject_id     AS subjectId,
                   difficulty     AS difficulty,
                   question_count AS questionCount,
                   overall_score  AS overallScore,
                   total_duration AS totalDuration,
                   started_at     AS startedAt,
                   completed_at   AS completedAt
            FROM interview_record
            WHERE delete_flag = 0 AND student_id = #{studentId}
            ORDER BY completed_at DESC, id DESC
            """)
    IPage<InterviewRecordItem> selectPageByStudent(IPage<InterviewRecordItem> page,
                                                   @Param("studentId") Long studentId);
}
