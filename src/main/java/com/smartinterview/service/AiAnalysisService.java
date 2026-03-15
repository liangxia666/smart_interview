package com.smartinterview.service;

import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import io.reactivex.Flowable;

import java.util.List;

public interface AiAnalysisService {
     Flowable<GenerationResult> streamAnalyzeResume(String rawText);

     Flowable<GenerationResult> streamChat(List<Message> messages);
}
