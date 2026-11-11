package com.yunshu.zhixue.modules.route.service.impl;

import com.yunshu.zhixue.modules.route.dto.RouteCompletionStat;
import com.yunshu.zhixue.modules.route.mapper.RouteCourseCompletionMapper;
import com.yunshu.zhixue.modules.route.service.RouteProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RouteProgressServiceImpl implements RouteProgressService {

    private final RouteCourseCompletionMapper routeCourseCompletionMapper;

    @Override
    public List<RouteCompletionStat> completedCountByStudents(Collection<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        List<Long> ids = studentIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        return routeCourseCompletionMapper.countByStudents(ids);
    }
}
