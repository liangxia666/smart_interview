package com.smartinterview;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.smartinterview.common.exception.BaseException;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.service.InterviewSessionService;
import com.smartinterview.service.impl.InterviewSessionServiceImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

@SpringBootTest
class SmartInterviewApplicationTests {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
    }
    @Test
    void testException(){
        throw new BaseException("测试异常");
    }
    @Test
    public void testExtractTextFromPdf() {
        // 1. 找一个你电脑本地的 PDF 简历路径进行测试
        String pdfFilePath = "C:\\Users\\32341\\Desktop\\360.pdf";
        File file = new File(pdfFilePath);

        PDDocument document = null;
        try {
            // 2. 加载 PDF 文档
            document = PDDocument.load(file);

            // 3. 实例化文本提取器
            PDFTextStripper stripper = new PDFTextStripper();

            // 按阅读顺序排序（对于简历这种多栏排版的文档非常重要）
            stripper.setSortByPosition(true);

            // 4. 执行提取
            String text = stripper.getText(document);

            System.out.println("====== 简历解析结果开始 ======");
            System.out.println(text);
            System.out.println("====== 简历解析结果结束 ======");

        } catch (IOException e) {
            System.err.println("PDF 解析失败: " + e.getMessage());
        } finally {
            // 5. 必须关闭文档释放内存
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Test
    public void testSendMessage(){
        String que="simple.queue";
        String msg="hello,amq";
        rabbitTemplate.convertAndSend(que,msg);
    }
    @Autowired
    private InterviewSessionServiceImpl interviewSessionService;
    @Test
    public void testDTO(){
        InterviewSession interviewSession=InterviewSession.builder().
                userId(2L).
                category("java后端开发").
                difficulty("无").
                createTime(LocalDateTime.now()).build();
        interviewSessionService.save(interviewSession);
    }
    @Test
    public void testRedis(){
        Message userMsg=Message.builder()
                .role(Role.USER.getValue())
                .content("你好这是我的简历")
                .build();
          //对象与字符串之间的转变：hutool工具包/ FastJSON（阿里开源 JSON 库）
//        // 1. 对象转 JSON 字符串
//        String jsonStr = JSONUtil.toJsonStr(message);
//
//        // 2. JSON 字符串转对象
//        Message msg = JSONUtil.toBean(jsonStr, Message.class);
        String jsonStr=JSONUtil.toJsonStr(userMsg);

        stringRedisTemplate.opsForList().rightPush("userMsg",jsonStr);
        String userMsg1 = stringRedisTemplate.opsForList().index("userMsg", 0);
        Message msg=JSONUtil.toBean(userMsg1,Message.class,false);
        System.out.println("userMsg1:"+msg.getContent());
    }

}
