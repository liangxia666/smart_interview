package com.smartinterview.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.entity.User;
import com.smartinterview.service.UserService;
import com.smartinterview.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
* @author 32341
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-02-26 16:36:05
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

}




