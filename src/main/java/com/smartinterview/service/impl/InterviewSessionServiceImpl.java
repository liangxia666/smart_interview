package com.smartinterview.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.service.InterviewSessionService;
import com.smartinterview.mapper.InterviewSessionMapper;
import org.springframework.stereotype.Service;

/**
* @author 32341
* @description 针对表【interview_session(面试会话主表)】的数据库操作Service实现
* @createDate 2026-02-26 16:36:05
*/
@Service
public class InterviewSessionServiceImpl extends ServiceImpl<InterviewSessionMapper, InterviewSession>
    implements InterviewSessionService{

}




