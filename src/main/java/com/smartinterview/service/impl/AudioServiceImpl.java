package com.smartinterview.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.smartinterview.service.AudioService;

import org.springframework.stereotype.Service;

@Service
public class AudioServiceImpl implements AudioService {
    // 替换为你自己在阿里云申请的参数
    private static final String APP_KEY = "你的AppKey";
    // 注意：实际项目中 Token 需要通过 AK 和 SK 动态获取并缓存,不是自己项目的token
    private static final String TOKEN = "你的有效Token";
    // 阿里云一句话识别 REST API 地址
    private static final String ASR_URL = "https://nls-gateway-cn-shanghai.aliyuncs.com/stream/v1/asr";
    public String convertToText(byte[] audioData){

        // 构建请求 URL，设置采样率（通常网页录音为 16000Hz）和格式（wav/pcm）
        String url = ASR_URL + "?appkey=" + APP_KEY + "&format=wav&sample_rate=16000";
        HttpResponse response = HttpRequest.post(url)
                .header("X-NLS-Token", TOKEN)
                .header("Content-Type", "application/octet-stream")
                .body(audioData)
                .timeout(5000) // 5秒超时
                .execute();

        if (response.isOk()) {
            // 解析阿里云返回的 JSON
            JSONObject jsonObject = JSONUtil.parseObj(response.body());
            if (jsonObject.getInt("status") == 20000000) { // 20000000 代表成功
                return jsonObject.getStr("result"); // 提取识别出的文字
            }
        }
        throw new RuntimeException("调用阿里云ASR失败: " + response.body());
    }
}


