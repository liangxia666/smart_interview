package com.smartinterview.common.util;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.smartinterview.common.exception.OssException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {
        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {

            log.error("OSS上传失败：{}", oe.getErrorMessage());
            throw new OssException("文件上传失败");
        } catch (ClientException ce) {
            log.error("OSS客户端异常：{}", ce.getMessage());
            throw new OssException("文件上传失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        try {
            //对文件名进行编码，要不浏览器访问url路径会报错
            String encodedObjectName = URLEncoder.encode(objectName, "UTF-8")
                    .replace("+", "%20");
            //文件访问路径规则 https://BucketName.Endpoint/ObjectName
            StringBuilder stringBuilder = new StringBuilder("https://");
            stringBuilder
                    .append(bucketName)
                    .append(".")
                    .append(endpoint)
                    .append("/")
                    .append(encodedObjectName);

            log.info("文件上传到:{}", stringBuilder.toString());

            return stringBuilder.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }


    }
}
