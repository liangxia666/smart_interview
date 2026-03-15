package com.smartinterview.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder  //自动生成全参构造器，spring不会自动生成无参构造方法
@NoArgsConstructor  //加上无参构造方法生成，builder的有参构造方法被覆盖
@AllArgsConstructor
public class ResumeReportVO {
    private String userReport;
    private String sort;
    private String systemSummary;
}
