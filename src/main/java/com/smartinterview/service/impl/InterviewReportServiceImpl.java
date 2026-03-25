package com.smartinterview.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.smartinterview.common.exception.InterviewSessionException;
import com.smartinterview.common.result.Result;
import com.smartinterview.entity.InterviewReport;
import com.smartinterview.entity.InterviewSession;
import com.smartinterview.mapper.InterviewReportMapper;
import com.smartinterview.mapper.InterviewSessionMapper;
import com.smartinterview.service.AiAnalysisService;
import com.smartinterview.service.InterviewReportService;

import com.smartinterview.service.InterviewSessionService;
import com.smartinterview.vo.InterviewReportVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Service
@Slf4j
public class InterviewReportServiceImpl extends ServiceImpl<InterviewReportMapper,InterviewReport>
        implements InterviewReportService {
    @Autowired
    private AiAnalysisService aiAnalysisService;
    @Autowired
    private InterviewSessionMapper interviewSessionMapper;

    /**
     * 生成报告对象，存到数据库
     * @param sessionId
     * @param messageId
     * @param aiQuestion
     * @param userAnswer
     * @param standardAnswer
     */
    @Async
    public void saveQuestionReport(Long sessionId,Long messageId,String aiQuestion,String userAnswer,String standardAnswer){
        try {
            //调用AI评分
            String aiRaw=aiAnalysisService.evaluateAnswer(aiQuestion,userAnswer,standardAnswer);
            log.info("AI单题评分结果：{}",aiRaw);
            //解析json
            int start=aiRaw.indexOf("{");
            int end = aiRaw.lastIndexOf("}");
            if(start==-1||end==-1){
                log.error("AI评分返回格式异常");
                return ;
            }
            //生成jsonObject对象，键值对格式的json串
            JSONObject json= JSONUtil.parseObj(aiRaw.substring(start,end+1));
            //将报告保存到数据库
            InterviewReport interviewReport = InterviewReport.builder()
                    .sessionId(sessionId)
                    .messageId(messageId)
                    .questionText(aiQuestion)
                    .aiRaw(aiRaw)
                    .userAnswer(userAnswer)
                    .standardAnswer(standardAnswer)
                    .score(json.getInt("score"))
                    .isCorrect(json.getBool("isCorrect"))
                    .comment(json.getStr("comment"))
                    .createTime(LocalDateTime.now())
                    .build();
            save(interviewReport);
            log.info("单体评分已保存,session={},score={}",sessionId,json.getInt("score"));
        } catch (Exception e) {
            log.error("saveQuestionReport 异常,sessionId={}",sessionId,e);
        }


    }

    /**
     * 生成完整报告，封装成VO返回
     * @param sessionId
     * @return
     */
    public InterviewReportVO buildReport(Long sessionId){
        //判断面试状态
        InterviewSession session=interviewSessionMapper.selectById(sessionId);
        if(session==null){
            throw new InterviewSessionException("面试记录不存在");
        }
        if(!session.getStatus().equals(Integer.valueOf(2))){
            throw new InterviewSessionException("面试未开始或还未完成，请先完成面试");
        }
        //获取报告内容
        LambdaQueryWrapper<InterviewReport> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(InterviewReport::getSessionId,sessionId)
                .ne(InterviewReport::getQuestionText, "") //过滤掉第一条问题记录 where 字段不为空
                .orderByAsc(InterviewReport::getId);
        List<InterviewReport> list=list(wrapper);
        InterviewReportVO interviewReportVO=new InterviewReportVO();
        interviewReportVO.setSessionId(sessionId);
        interviewReportVO.setQuestionCount(list.size());
       //如果查不到数据
        if(list.isEmpty()){
            interviewReportVO.setTotalScore(0);
            interviewReportVO.setCorrectCount(0);
            interviewReportVO.setCorrectRate("0%");
            interviewReportVO.setItems(List.of());
            return interviewReportVO;
        }
        int totalScore =(int) Math.round(  //将总分四舍五入
                //将集合元素的score字段转为int 成为intStream
                list.stream().mapToInt(r-> r.getScore()==null?0:r.getScore())
                        .average().orElse(0)); //流为空时设为0
                long correctCount=list.stream()
                        //只保留作对的题的流
                        .filter(r-> Boolean.TRUE.equals(r.getIsCorrect()))
                        .count();//计算数量
       //计算答对比例 80.2% 两个%%输出%
        String correctRate=String.format("%.1f%%",correctCount*100.0/list.size());

        interviewReportVO.setTotalScore(totalScore);
        interviewReportVO.setCorrectRate(correctRate);
        interviewReportVO.setCorrectCount((int)correctCount);
        List<InterviewReportVO.QuestionReportItem> items=list.stream()
                //将list集合中的元素经过转换规则转为另一种元素
                .map(r->{
                    InterviewReportVO.QuestionReportItem item=new InterviewReportVO.QuestionReportItem();
                    item.setQuestionText(r.getQuestionText());
                    item.setComment(r.getComment());
                    item.setIsCorrect(r.getIsCorrect());
                    item.setUserAnswer(r.getUserAnswer());
                    item.setScore(r.getScore());
                    return item;
                }).collect(Collectors.toList());
        interviewReportVO.setItems(items);
        return interviewReportVO;
    }
    public void exportReport(Long sessionId, HttpServletResponse response){
        InterviewSession session=interviewSessionMapper.selectById(sessionId);
        if(session==null||!session.getStatus().equals(Integer.valueOf(2))){
            throw new InterviewSessionException("面试不存在或未完成");
        }
        // 1. 查报告数据
        List<InterviewReport> list = lambdaQuery()
                .eq(InterviewReport::getSessionId, sessionId)
                .orderByAsc(InterviewReport::getId)
                .list();

        // 2. 计算汇总数据
        int totalScore =  (int) Math.round(
                list.stream().mapToInt(r -> r.getScore()==null?0:r.getScore())
                        .average().orElse(0));
        //计算正确的数量
        long correctCount=list.stream()
                .filter(r->Boolean.TRUE.equals(r.getIsCorrect())).count();
        //计算正确率 %.1f%%相当于2.5%
        String correctRate = String.format("%.1f%%", correctCount * 100.0 / list.size());
       // 创建一个内存里的字节数组输出流
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            //创建一个个PDF 书写器，PDF里面的内容写入内存输出流
            PdfWriter writer = new PdfWriter(baos);
            //创建pdf文档对象
            PdfDocument pdf = new PdfDocument(writer);
            //创建文本内容容器
            Document document = new Document(pdf, PageSize.A4);
            //设置边距
            document.setMargins(40, 50, 40, 50);



            // 使用 ClassPathResource 加载，兼容 Jar 包部署环境

            PdfFont chineseFont = loadTtcFont("fonts/msyh.ttc", 0);
            PdfFont boldFont = loadTtcFont("fonts/msyhbd.ttc", 0);
            // ── 标题 ──
            document.add(new Paragraph(session.getTitle() + " · 面试报告")
                    .setFont(boldFont)//字体
                    .setFontSize(20)//字号
                    .setBold() //加粗
                    .setTextAlignment(TextAlignment.CENTER) //居中
                    .setMarginBottom(8)); //底部间距

            // 面试时间
            document.add(new Paragraph("面试时间：" +
                    session.getCreateTime().format(
                            DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm")))
                    .setFont(chineseFont) //格式
                    .setFontSize(10) //大小
                    .setFontColor(ColorConstants.GRAY) //文本字体颜色
                    .setTextAlignment(TextAlignment.CENTER) //居中
                    .setMarginBottom(20));   //底部间距

            // ── 分隔线 ──实线
            document.add(new LineSeparator(new SolidLine(0.5f))
                    .setMarginBottom(20));

            // ── 综合评分区域 ──
            document.add(new Paragraph("综合评估")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setBold()
                    .setMarginBottom(12));

            // 评分表格 创建4列表格
            Table scoreTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                    .useAllAvailableWidth() //占满页宽
                    .setMarginBottom(24);

            // 创建表头
            for (String header : new String[]{"综合得分", "答题数量", "答对数量", "正确率"}) {
                scoreTable.addHeaderCell(new Cell()
                        .add(new Paragraph(header) //表头文本
                                .setFont(boldFont)
                                .setFontSize(11)
                                .setBold()
                                .setTextAlignment(TextAlignment.CENTER))
                        .setBackgroundColor(new DeviceRgb(59, 130, 246))  // 蓝色表头
                        .setFontColor(ColorConstants.WHITE)
                        .setPadding(10));//内间距
            }

            // 数据行
            for (String value : new String[]{
                    totalScore + " 分",
                    list.size() + " 题",
                    correctCount + " 题",
                    correctRate}) {
                scoreTable.addCell(new Cell()
                        .add(new Paragraph(value)
                                .setFont(chineseFont)
                                .setFontSize(13)
                                .setBold()
                                .setTextAlignment(TextAlignment.CENTER))
                        .setPadding(12)
                        .setTextAlignment(TextAlignment.CENTER));
            }
            document.add(scoreTable);

            // ── 题目详情 ──
            document.add(new Paragraph("题目详情")
                    .setFont(boldFont)
                    .setFontSize(14)
                    .setBold()
                    .setMarginBottom(12));

            for (int i = 0; i < list.size(); i++) {
                InterviewReport report = list.get(i);
                boolean correct = Boolean.TRUE.equals(report.getIsCorrect());

                // 题目编号 + 正确/错误标签
                Paragraph questionTitle = new Paragraph()
                        .add(new Text("Q" + (i + 1) + "  ")
                                .setFont(boldFont)
                                .setFontSize(12)
                                .setBold())
                        .add(new Text(correct ? "正确 " : "错误 ")
                                .setFont(chineseFont)
                                .setFontSize(10)
                                .setFontColor(ColorConstants.WHITE)
                                .setBackgroundColor(correct
                                        ? new DeviceRgb(34, 197, 94)   // 绿色
                                        : new DeviceRgb(239, 68, 68))) // 红色
                        .setMarginBottom(6);
                document.add(questionTitle);

                // 问题内容
                document.add(new Paragraph("面试问题：" +
                        (StrUtil.isNotBlank(report.getQuestionText())
                                ? report.getQuestionText() : "暂无"))
                        .setFont(chineseFont)
                        .setFontSize(11)
                        .setFontColor(new DeviceRgb(30, 30, 30))
                        .setMarginBottom(4));

                // 用户回答
                document.add(new Paragraph("我的回答：" +
                        (StrUtil.isNotBlank(report.getUserAnswer())
                                ? report.getUserAnswer() : "未作答"))
                        .setFont(chineseFont)
                        .setFontSize(10)
                        .setFontColor(new DeviceRgb(80, 80, 80))
                        .setMarginBottom(4));

                // AI 评价 + 得分
                document.add(new Paragraph(
                        "AI 评价：" + (StrUtil.isNotBlank(report.getComment())
                                ? report.getComment() : "暂无评价") +
                                "    得分：" + (report.getScore() != null
                                ? report.getScore() + " 分" : "-"))
                        .setFont(chineseFont)
                        .setFontSize(10)
                        .setFontColor(new DeviceRgb(100, 100, 100))
                        .setMarginBottom(4));

                // 题目之间加分隔线 dash虚线
                if (i < list.size() - 1) {
                    document.add(new LineSeparator(new DashedLine(0.5f))
                            .setMarginTop(8)
                            .setMarginBottom(12));
                }
            }

            // ── 页脚 ──
            document.add(new LineSeparator(new SolidLine(0.5f)).setMarginTop(20));
            document.add(new Paragraph("由 Smart Interview 智能面试系统生成")
                    .setFont(chineseFont)
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(8));

            document.close();
            // 只有到这一步没报错，才设置 header 并写回浏览器
            //将pdf转成字节数组
            byte[] content = baos.toByteArray();
            //设置响应头，告知前端是pdf
            response.setContentType("application/pdf");
            // 文件名处理：加上 .pdf 后缀，并处理空格
            String fileName = URLEncoder.encode(session.getTitle() + "-面试报告.pdf", "UTF-8").replaceAll("\\+", "%20");
           // 告诉浏览器：弹出下载框，不要直接打开，这是国际标准的下载格式
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
           //告诉浏览器文件大小，显示下载进度
            response.setContentLength(content.length);
            //将pdf写给响应对象的输出流
            response.getOutputStream().write(content);
            //开始下载
            response.getOutputStream().flush();
            log.info("面试报告 PDF 导出成功，sessionId={}", sessionId);

        } catch (Exception e) {
            log.error("PDF 导出失败，sessionId={}", sessionId, e);
            throw new RuntimeException("PDF 导出失败，请重试");
        }

    }


    /**
     * 专门用于在 Spring Boot 环境下加载 TTC 字体
     */
    private PdfFont loadTtcFont(String classPath, int index) throws IOException {
        // 1. 获取 resources 下的字体输入流
        try (InputStream is = new ClassPathResource(classPath).getInputStream()) {
            // 2. 在服务器操作系统生成一个临时文件
            //因为 iText 对 TTC 字体集合 的支持有一个限制：
            //必须用 文件系统的绝对路径 + , 索引 才能加载
            File tempFile = File.createTempFile("temp_font_", ".ttc");
            tempFile.deleteOnExit(); // JVM 停止时自动删除，防止占用磁盘

            // 3. 将字体文件流写入临时文件   ，临时文件就有字体数据
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                StreamUtils.copy(is, fos);
            }

            // 4. 让 iText 通过绝对路径读取，并完美支持 ",0" 语法
            String fontPathWithIndex = tempFile.getAbsolutePath() + "," + index;
            //返回创建的字体
            return PdfFontFactory.createFont(
                    fontPathWithIndex,  //字体格式 传入字符串的话，会自动截取  路径+0，如果传字节数组不行
                    PdfEncodings.IDENTITY_H, //创建Unicode编码
                    PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED //把字体嵌入PDF
            );
        }
    }

}
