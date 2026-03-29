package com.smartinterview.controller;

import com.alibaba.excel.EasyExcel;
import com.smartinterview.Listener.QuestionDataListener;
import com.smartinterview.common.result.Result;
import com.smartinterview.dto.QuestionImportDTO;
import com.smartinterview.service.SysQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class ImportData {
    @Autowired
    private SysQuestionService sysQuestionService;
    @PostMapping("/import")
    public Result<String> importQuestions(MultipartFile file) throws IOException {
        EasyExcel.read(file.getInputStream(), //获取文件输入流
                QuestionImportDTO.class,  //将每行的数据转成对用的实体类
                new QuestionDataListener(sysQuestionService)) //数据读取后的处理监听器
                .sheet() //默认读取excel的第一个文件
                .doRead(); //执行读取操作
        return Result.success("数据导入成功");
    }
}
