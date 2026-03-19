package com.smartinterview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartinterview.common.result.Result;
import com.smartinterview.entity.InterviewReport;

public interface InterviewReportService extends IService<InterviewReport> {
   void saveQuestionReport(Long sessionId,Long messageId,String aiQuestion,String userAnswer,String standardAnswer);
    Result buildReport(Long sessionId);
}
