package com.yunshu.zhixue.modules.course.service;

import com.yunshu.zhixue.common.response.PageResult;
import com.yunshu.zhixue.modules.course.dto.CourseItem;
import com.yunshu.zhixue.modules.course.dto.CourseQuery;
import com.yunshu.zhixue.modules.course.dto.CourseSaveRequest;
import com.yunshu.zhixue.modules.course.dto.CourseStatsResponse;

/**
 * B 站课程库服务（A-C01 ~ A-C05）。
 */
public interface CourseService {

    CourseStatsResponse stats();

    PageResult<CourseItem> pageCourses(CourseQuery query);

    Long createCourse(CourseSaveRequest request);

    void updateCourse(Long courseId, CourseSaveRequest request);

    void deleteCourse(Long courseId);
}
