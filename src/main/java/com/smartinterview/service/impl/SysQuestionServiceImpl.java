package com.smartinterview.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.entity.SysQuestion;
import com.smartinterview.service.SysQuestionService;
import com.smartinterview.mapper.SysQuestionMapper;
import org.springframework.stereotype.Service;

/**
* @author 32341
* @description 针对表【sys_question(系统面试题库)】的数据库操作Service实现
* @createDate 2026-02-26 16:36:05
*/
@Service
public class SysQuestionServiceImpl extends ServiceImpl<SysQuestionMapper, SysQuestion>
    implements SysQuestionService{

    public String searchStanderAnswer(String userMessage){
        if(StrUtil.isBlank(userMessage)){
            return null;
        }
        //防止sql拼接错误
        String safeKeyword = userMessage.replace("'","");
        QueryWrapper<SysQuestion> wrapper=new QueryWrapper<>();
        //last拼接到sql语句最后，match全文索引匹配，默认自然语言模式

        wrapper.apply(" match(question,answer) against ({0} in natural language mode) ",safeKeyword);
        wrapper.last("limit 1");
        SysQuestion matchedQuestion=getOne(wrapper);
        if(matchedQuestion!=null){
            return "【原题】：" + matchedQuestion.getQuestion() + "\n【标准答案】：" + matchedQuestion.getAnswer();
        }
        return null;
    }
}




