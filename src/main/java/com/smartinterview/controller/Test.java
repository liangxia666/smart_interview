package com.smartinterview.controller;

import com.smartinterview.common.exception.BaseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("test")
public class Test {
    @GetMapping("exception")
    public void testException(){
        throw new BaseException("controller测试异常");
    }
}
