package com.smartinterview.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartinterview.common.constants.RedisConstants;
import com.smartinterview.common.exception.CodeException;
import com.smartinterview.common.exception.PhoneException;
import com.smartinterview.common.result.Result;
import com.smartinterview.common.result.ResultCode;
import com.smartinterview.common.util.JwtProperties;
import com.smartinterview.common.util.JwtUtil;
import com.smartinterview.common.util.RegexUtils;
import com.smartinterview.dto.LoginDTO;
import com.smartinterview.entity.User;
import com.smartinterview.service.UserService;
import com.smartinterview.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.bean.BeanUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
    private JwtProperties jwtProperties;
    @Autowired
    private UserMapper userMapper;

    /**
     * 发送验证码
     * @param phone
     * @return
     */
    @Override
    public Result sendMessage(String phone) {

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
            return Result.error("请稍后重试");
        }
        redisTemplate.opsForValue().set(codeKey,s,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.info("发送验证码成功：{}",s);
        return Result.success();
    }

    @Override
    public Result login(LoginDTO loginDTO) {
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
        Map<String,Object> map= BeanUtil.beanToMap(user);
        //只存nickname id就行
//        Map<String,String> map=new HashMap<>();
//        map.put("userId",user.getId().toString());
//        map.put("nickname",user.getNickname());
        redisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER+token,map);
        redisTemplate.expire(RedisConstants.LOGIN_USER+token,RedisConstants.LOGIN_TOKEN_TTL,TimeUnit.MINUTES);
        return Result.success(token);

    }
    public User createUser(String phone){
        User user=new User();
        user.setPhone(phone);
        user.setNickname(RedisConstants.USER_NICK_NAME+RandomUtil.randomString(10));
        save(user);
        return user;
    }
    public Result logout(String token){
        String userKey=RedisConstants.LOGIN_USER+token;
        redisTemplate.delete(userKey);
        return Result.success();
    }
}




