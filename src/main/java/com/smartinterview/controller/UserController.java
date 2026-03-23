package com.smartinterview.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.smartinterview.common.constants.RedisConstants;
import com.smartinterview.common.result.Result;
import com.smartinterview.common.util.AliOssUtil;
import com.smartinterview.common.util.RegexUtils;
import com.smartinterview.common.util.UserHolder;
import com.smartinterview.dto.LoginDTO;
import com.smartinterview.dto.UpdateUserDTO;
import com.smartinterview.dto.UserDTO;
import com.smartinterview.entity.User;
import com.smartinterview.service.UserService;
import com.smartinterview.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.Request;
import org.apache.ibatis.annotations.Update;
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
         userService.sendMessage(phone);
        return Result.success();

    }
    @Operation(summary="用户登录")
    @PostMapping("login")
    public Result login(@RequestBody LoginDTO loginDTO){
        String token = userService.login(loginDTO);
        return Result.success(token);
    }

    @Operation(summary="用户退出")
    @PostMapping("logout")
    public Result logout(HttpServletRequest request){
        String token=request.getHeader("Authorization");
        userService.logout(token);
        return Result.success();
    }
    @Operation(summary="查询用户信息")
    @GetMapping("me")
    public Result getUser(){
      UserVO userVO=  userService.queryUser();
        return Result.success(userVO);
    }
    @Operation(summary="上传头像")
    @PostMapping("avatar")
    public Result uploadAvatar(@RequestParam(defaultValue="file") MultipartFile file){
        String url=userService.uploadAvatar(file);
        return Result.success(url);
    }
    @Operation(summary="修改用户信息")
    @PutMapping("/update")
    public Result updateUser(@RequestBody UpdateUserDTO userDTO, HttpServletRequest request){
      userService.updateUser(userDTO,request);
      return Result.success();
    }


}
