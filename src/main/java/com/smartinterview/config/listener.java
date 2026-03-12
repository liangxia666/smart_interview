package com.smartinterview.config;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component

public class listener {
    @RabbitListener(queues="simple.queue")
    public void getMessage(String msg){
        System.out.println("消费者收到消息："+msg);
    }
}
