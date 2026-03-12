package com.smartinterview.controller;

import cn.hutool.core.util.RandomUtil;
import com.smartinterview.common.constants.RedisConstants;
import com.smartinterview.common.result.Result;
import com.smartinterview.common.util.AliOssUtil;
import com.smartinterview.common.util.RegexUtils;
import com.smartinterview.dto.LoginDTO;
import com.smartinterview.entity.User;
import com.smartinterview.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("user")
@Tag(name = "用户中心")
public class UserController {

    @Autowired
    private UserService userService;
    @Operation(summary="发送验证码")
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone){
        return userService.sendMessage(phone);

    }
    @Operation(summary="用户登录")
    @PostMapping("login")
    public Result login(@RequestBody LoginDTO loginDTO){
        return userService.login(loginDTO);
    }

    @Operation(summary="用户退出")
    @PostMapping("logout")
    public Result logout(HttpServletRequest request){
        String token=request.getHeader("Authorization");
       return userService.logout(token);
    }

}
