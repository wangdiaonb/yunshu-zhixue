package com.yunshu.zhixue.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 操作日志表 {@code operation_log}（无逻辑删除，清空即物理删除）。
 */
@Getter
@Setter
@TableName("operation_log")
public class OperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 用户名快照 */
    private String username;

    /** 登录/登出/新增/修改/删除/禁用启用/导出/其他 */
    private String action;

    private String targetType;

    private String targetName;

    private String ip;

    private LocalDateTime createdAt;
}
