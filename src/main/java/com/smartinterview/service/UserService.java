package com.smartinterview.service;

import com.smartinterview.dto.LoginDTO;
import com.smartinterview.dto.UpdateUserDTO;
import com.smartinterview.dto.UserDTO;
import com.smartinterview.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartinterview.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 32341
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2026-02-26 16:36:05
*/
public interface UserService extends IService<User> {

    void sendMessage(String phone);

    String login(LoginDTO loginDTO);

    void logout(String token);

    String uploadAvatar(MultipartFile file);

    void updateUser(UpdateUserDTO userDTO, HttpServletRequest request);

    UserVO queryUser();
}
