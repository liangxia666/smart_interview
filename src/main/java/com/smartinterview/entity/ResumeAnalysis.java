package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * @TableName resume_analysis
 */
@TableName(value ="resume_analysis")
@Data
public class ResumeAnalysis {
    private Long id;

    private Long userId;

    private String fileUrl;

    private String jobIntention;

    private String originalText;

    private Integer status;

    private Integer score;

    private String summary;

    private String suggestion;

    private Date createTime;

    private Date updateTime;

    private Integer isDeleted;
}