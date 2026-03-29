package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @TableName sys_question
 */
@TableName(value ="sys_question")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysQuestion {
    private Long id;

    private String category;


    private String question;

    private String answer;

    private Integer difficulty;
    /**
     * 向量 JSON 字符串，通义千问 text-embedding-v3 生成，1024 维
     * 格式：[0.123, -0.456, ...]
     * 存储在 MySQL MEDIUMTEXT 列
     */
    private String embedding;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;
}