package com.yunshu.zhixue.modules.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.question.entity.Question;
import com.yunshu.zhixue.modules.question.support.QuestionPracticeStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目 Mapper。
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /** 被练习明细 + 错题本引用的次数（删除提示）。 */
    @Select("""
            SELECT (SELECT COUNT(*) FROM practice_record
                    WHERE delete_flag = 0 AND question_id = #{questionId})
                 + (SELECT COUNT(*) FROM wrong_question
                    WHERE delete_flag = 0 AND question_id = #{questionId})
            """)
    long countReferences(@Param("questionId") Long questionId);

    @Select("""
            <script>
            SELECT question_id AS questionId,
                   COUNT(*) AS doneCount,
                   COALESCE(SUM(is_correct), 0) AS correctCount
            FROM practice_record
            WHERE delete_flag = 0
              AND question_id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY question_id
            </script>
            """)
    List<QuestionPracticeStat> selectPracticeStats(@Param("ids") List<Long> ids);
}
