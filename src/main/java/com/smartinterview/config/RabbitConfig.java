package com.smartinterview.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



@Configuration
public class RabbitConfig {
    public static final String RESUME_PARSE_QUEUE="resume_parse_queue";
    public static final String RESUME_PARSE_EXCHANGE="resume_parse_exchange";
    public static final String RESUME_ROUTING_KEY="resume_routing_key";
   //开启队列持久化
   @Bean
    public Queue resumeParseQueue(){
        return new Queue(RESUME_PARSE_QUEUE,true);
    }
    //交换机持久化，关闭自动删除
    @Bean
    public DirectExchange directExchange(){
       return new DirectExchange(RESUME_PARSE_EXCHANGE,true,false);
    }
    //队列绑定到交换机
    @Bean
    public Binding binding(Queue resumeParseQueue ,DirectExchange directExchange){
       return BindingBuilder.bind(resumeParseQueue).to(directExchange).with(RESUME_ROUTING_KEY);
    }
}
