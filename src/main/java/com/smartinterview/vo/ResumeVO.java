package com.smartinterview.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder  //自动生成全参构造器，spring不会自动生成无参构造方法
@NoArgsConstructor  //加上无参构造方法生成，builder的有参构造方法被覆盖
@AllArgsConstructor
public class ResumeVO {
    private Long id;
    private String fileUrl;       // 简历文件链接
    private Integer status;       // 解析状态
    private String score;         // AI评分
    private LocalDateTime createTime;
}
