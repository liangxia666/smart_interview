package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName interview_session
 */
@TableName(value ="interview_session")
@Data
public class InterviewSession {
    private Long id;

    private Long userId;

    private Long resumeId;

    private String category;

    private String difficulty;

    private String title;

    private Date createTime;

    private Date updateTime;

    private Integer isDeleted;
}