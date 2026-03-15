package com.smartinterview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @TableName user
 */
@TableName(value ="user")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private String openid;

    private String phone;

    private Date createTime;

    private Date updateTime;

    private Integer isDeleted;
}