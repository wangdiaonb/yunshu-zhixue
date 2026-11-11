package com.yunshu.zhixue.modules.interviewbank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.interviewbank.entity.InterviewQuestion;
import com.yunshu.zhixue.modules.interviewbank.support.TopicCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 面试题 Mapper。
 */
@Mapper
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestion> {

    /**
     * 按主题聚合题目数（未分类题目不计入），用于主题下拉的题数展示。
     */
    @Select("""
            <script>
            SELECT topic AS topic, COUNT(*) AS questionCount
            FROM interview_question
            WHERE delete_flag = 0 AND topic IS NOT NULL AND topic &lt;&gt; ''
            <if test="subjectId != null">AND subject_id = #{subjectId}</if>
            GROUP BY topic
            </script>
            """)
    List<TopicCount> countByTopic(@Param("subjectId") Long subjectId);
}
