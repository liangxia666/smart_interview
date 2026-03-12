package com.smartinterview;

import com.smartinterview.common.exception.BaseException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;

@SpringBootTest
class SmartInterviewApplicationTests {

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

}
