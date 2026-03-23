package com.smartinterview.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult implements Serializable {
    private Long total;       // 总记录数
    private Long pages;       // 总页数
    private Integer current;     // 当前页码
    private Long size;        // 每页条数

    private List records; //当前页数据集合

}
