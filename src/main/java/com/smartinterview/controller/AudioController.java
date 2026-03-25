package com.smartinterview.controller;

import com.smartinterview.common.result.Result;

import com.smartinterview.service.AudioService;
import com.smartinterview.service.impl.AudioServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("audio")
public class AudioController {
    @Autowired
    private AudioService audioService;
    @PostMapping("/recognise")
    public Result<String> recognizeAudio(@RequestParam("audio")MultipartFile file){
        if(file.isEmpty()){
            return Result.error("音频文件不能为空");

        }
        try {
            String text=audioService.convertToText(file.getBytes());
            return Result.success(text);
        } catch (Exception e) {
            return Result.error("语音转换失败"+e.getMessage());
        }

    }
}
