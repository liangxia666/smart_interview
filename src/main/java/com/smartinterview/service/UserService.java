package com.smartinterview.service;

import com.smartinterview.common.result.Result;
import com.smartinterview.dto.LoginDTO;
import com.smartinterview.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 32341
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2026-02-26 16:36:05
*/
public interface UserService extends IService<User> {

    Result sendMessage(String phone);

    Result login(LoginDTO loginDTO);
}
