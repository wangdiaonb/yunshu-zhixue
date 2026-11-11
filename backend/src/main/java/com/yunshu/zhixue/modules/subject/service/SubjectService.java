package com.yunshu.zhixue.modules.subject.service;

import com.yunshu.zhixue.modules.subject.dto.StageSaveRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectCard;
import com.yunshu.zhixue.modules.subject.dto.SubjectOption;
import com.yunshu.zhixue.modules.subject.dto.SubjectSaveRequest;
import com.yunshu.zhixue.modules.subject.dto.SubjectStatsResponse;

import java.util.List;

/**
 * 学科与阶段管理服务（A-S01 ~ A-S11）。
 */
public interface SubjectService {

    SubjectStatsResponse stats();

    List<SubjectCard> listSubjects();

    /** 学科下拉选项（按排序号）。 */
    List<SubjectOption> listOptions();

    Long createSubject(SubjectSaveRequest request);

    void updateSubject(Long subjectId, SubjectSaveRequest request);

    /** 按 orderedIds 顺序重写全部学科的 sort_order。 */
    void sortSubjects(List<Long> orderedIds);

    void deleteSubject(Long subjectId);

    Long createStage(Long subjectId, StageSaveRequest request);

    void updateStage(Long subjectId, Long stageId, StageSaveRequest request);

    /** 按 orderedIds 顺序重写指定学科下全部阶段的 sort_order。 */
    void sortStages(Long subjectId, List<Long> orderedIds);

    void updateStageStatus(Long subjectId, Long stageId, Integer status);

    void deleteStage(Long subjectId, Long stageId);
}
