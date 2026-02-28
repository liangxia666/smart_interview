package com.smartinterview;

import com.smartinterview.common.exception.BaseException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmartInterviewApplicationTests {

    @Test
    void contextLoads() {
    }
    @Test
    void testException(){
        throw new BaseException("测试异常");
    }

}
