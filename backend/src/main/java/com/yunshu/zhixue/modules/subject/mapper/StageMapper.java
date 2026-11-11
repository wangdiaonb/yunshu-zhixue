package com.yunshu.zhixue.modules.subject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yunshu.zhixue.modules.subject.entity.Stage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 阶段 Mapper。
 */
@Mapper
public interface StageMapper extends BaseMapper<Stage> {

    /** 阶段下题目数（删除保护与卡片展示）。 */
    @Select("SELECT COUNT(*) FROM question WHERE delete_flag = 0 AND stage_id = #{stageId}")
    long countQuestions(@Param("stageId") Long stageId);
}
