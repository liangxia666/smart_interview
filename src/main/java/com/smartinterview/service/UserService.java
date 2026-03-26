package com.smartinterview.service;

import com.smartinterview.dto.CodeLoginDTO;
import com.smartinterview.dto.PasswordLoginDTO;
import com.smartinterview.dto.RegisterDTO;
import com.smartinterview.dto.UpdateUserDTO;
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

    String sendMessage(String phone);
    void register(RegisterDTO registerDTO);

    String codeLogin(CodeLoginDTO codeLoginDTO);
    String passwordLogin(PasswordLoginDTO passwordLoginDTO);


    void logout(String token);

    String uploadAvatar(MultipartFile file);

    void updateUser(UpdateUserDTO userDTO, HttpServletRequest request);

    UserVO queryUser();



}
