package com.smartinterview.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    /**
     * 文档信息
     * @return
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智面Rush - AI高并发智能模拟面试平台")
                        .version("1.0")
                        .description("支持简历智能诊断 + AI流式模拟面试 + 高并发秒杀抢约")
                        .contact(new Contact()
                                .name("侯光龙")
                                .email("323412916@qq.com")
                        )
                );
    }

    // 全局JWT参数（登录后直接在文档里输入token调试）
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")
                .pathsToMatch("/**")
                .addOperationCustomizer((operation, handlerMethod) -> {
                    operation.addParametersItem(new io.swagger.v3.oas.models.parameters.Parameter()
                            .in("header")
                            .name("Authorization")
                            .description("Bearer {token}")
                            .required(false));
                    return operation;
                })
                .build();
    }
}