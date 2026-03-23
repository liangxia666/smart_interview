package com.smartinterview.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.common.constants.RedisConstants;
import com.smartinterview.common.exception.AvatarException;
import com.smartinterview.common.exception.CodeException;
import com.smartinterview.common.exception.PhoneException;
import com.smartinterview.common.util.*;
import com.smartinterview.dto.LoginDTO;
import com.smartinterview.dto.UpdateUserDTO;
import com.smartinterview.dto.UserDTO;
import com.smartinterview.entity.User;
import com.smartinterview.service.UserService;
import com.smartinterview.mapper.UserMapper;
import com.smartinterview.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
* @author 32341
* @description 针对表【user(用户表)】的数据库操作Service实现
* @createDate 2026-02-26 16:36:05
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AliOssUtil aliOssUtil;
    private static final String DEFAULT_AVATAR="https://smart-interv.oss-cn-beijing.aliyuncs.com/avatar/ac118ea7-8b83-4ade-a2a9-a071b62440ae-默认头像.png";

    /**
     * 发送验证码
     *
     * @param phone
     */
    @Override
    public void sendMessage(String phone) {


        //无效返回true
        if(RegexUtils.isPhoneInvalid(phone)){
            throw new PhoneException("手机号格式错误");
        }
        String s = RandomUtil.randomNumbers(6);
        String codeKey= RedisConstants.LOGIN_CODE_KEY+phone;
        String codeRate=RedisConstants.CODE_RATE_KEY+phone;
        Long count = redisTemplate.opsForValue().increment(codeRate, 1);
        if(count==1){
            redisTemplate.expire(codeRate,60,TimeUnit.SECONDS);
        }
        if(count>3){
            throw new CodeException("失败次数太多，请稍后重试");
        }
        redisTemplate.opsForValue().set(codeKey,s,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info("发送验证码成功：{}",s);

    }

    @Override
    public String login(LoginDTO loginDTO) {
        String phone=loginDTO.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
          throw new PhoneException("手机号格式错误");
        }
        Object o= redisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY+phone);
        String code = String.valueOf(o);
        if(code==null||!code.equals(loginDTO.getCode())){
            throw new CodeException("验证码错误");
        }
        User user=query().eq("phone",loginDTO.getPhone()).one();
        if(user==null){
          user=  createUser(phone);
        }
        HashMap<String,Object> claim=new HashMap<>();
        claim.put(RedisConstants.CLAIM_USER_ID,user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretkey(), jwtProperties.getUserTtl(), claim);
//        redisTemplate.opsForValue().set("login_token:key:"+user.getId(),token);
       // stringRedisTemplate默认传入String类型
       // Map<String,Object> map= BeanUtil.beanToMap(user);
        //只存nickname id,就行
        Map<String,String> map=new HashMap<>();
        map.put("userId",user.getId().toString());
        map.put("nickname",user.getNickname());
        redisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER+token,map);
        redisTemplate.expire(RedisConstants.LOGIN_USER+token,RedisConstants.LOGIN_TOKEN_TTL,TimeUnit.MINUTES);
        return token;

    }
    public User createUser(String phone){
        User user=new User();
        user.setPhone(phone);
        user.setNickname(RedisConstants.USER_NICK_NAME+RandomUtil.randomString(10));
        user.setAvatar(DEFAULT_AVATAR);
        save(user);
        return user;
    }
    public void logout(String token){
        String userKey=RedisConstants.LOGIN_USER+token;
        redisTemplate.delete(userKey);

    }
    public String uploadAvatar(MultipartFile file){
        String originalFilename=file.getOriginalFilename();
        if (originalFilename == null ||
                !originalFilename.matches(".*\\.(jpg|jpeg|png|gif)$")) {
            throw new AvatarException("仅支持 jpg/jpeg/png/gif 格式");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new AvatarException("图片不能超过 5MB");
        }
        try {
            String fileName="avatar/"+ UUID.randomUUID()+"-"+originalFilename;
            String url=aliOssUtil.upload(file.getBytes(), fileName);
            return url;
        } catch (IOException e) {
            log.error("上传头像失败",e);
            throw new AvatarException("上传头像失败");
        }

    }

    @Override
    public UserVO queryUser() {
        UserDTO userDTO=UserHolder.getUser();
        UserVO userVO= UserVO.builder()
                .id(userDTO.getId())
                .avatar(userDTO.getAvatar())
                .nickName(userDTO.getNickname())  //\\d{3}查到3个数字 ，（）用$1替换
                .phone(userDTO.getPhone().replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"))
                .build();
        return userVO;
    }

    @Override
    public void updateUser(UpdateUserDTO userDTO, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        Long userId= UserHolder.getUser().getId();
        User user=new User();
        user.setId(userId);
        user.setNickname(userDTO.getNickname());
        user.setAvatar(userDTO.getAvatar());
        updateById(user);
        //更新redis
        String redisKey=RedisConstants.LOGIN_USER+token;
        stringRedisTemplate.opsForHash().put(redisKey,"nickname",userDTO.getNickname());

    }
}




