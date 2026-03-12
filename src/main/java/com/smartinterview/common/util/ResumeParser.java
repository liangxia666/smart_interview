package com.smartinterview.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;

@Slf4j
@Component
public class ResumeParser {

    /**
     * 根据 PDF 文件的 URL 地址（通常是 OSS 的外链），解析出其中的文本内容
     * @param fileUrl PDF 文件的网络路径
     * @return 提取出的纯文本字符串
     */
    public String parsePdfFromUrl(String fileUrl) {
        log.info("开始解析 PDF 文件，路径: {}", fileUrl);
        
        // 1. 获取网络输入流并加载 PDF 文档
        // 使用 BufferedInputStream 包装 URL 流，提高读取效率
        try (InputStream inputStream = new URL(fileUrl).openStream();
             BufferedInputStream bis = new BufferedInputStream(inputStream);
             PDDocument document = PDDocument.load(bis)) {

            // 2. 实例化 PDF 文本提取器
            PDFTextStripper stripper = new PDFTextStripper();


            // 设置为 true 后，PDFBox 会尝试按照人类阅读顺序（从上到下、从左到右）提取
            stripper.setSortByPosition(true);

            // 4. 执行提取操作
            String result = stripper.getText(document);
            
            log.info("PDF 解析成功，字数: {}", result != null ? result.length() : 0);
            return result;

        } catch (Exception e) {
            log.error("PDF 解析失败，地址: {}", fileUrl, e);
            throw new RuntimeException("简历内容解析异常，请检查文件格式或重试");
        }
    }
}