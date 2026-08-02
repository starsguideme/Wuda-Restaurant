package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    //微信登录接口地址
    public static final String WX_LOGIN= "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获取当前用户的OpenID
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", userLoginDTO.getCode());
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        log.info("微信接口返回的json数据：{}", json);
       JSONObject jsonObject = JSONObject.parseObject(json);
       String openid = jsonObject.getString("openid");
       String sessionKey = jsonObject.getString("session_key");
        //判断当前OpenID是否异常 如果是则抛出异常
        if(openid == null){
            log.error("微信接口返回的OpenID为空");
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断当前用户是否是新用户
        User user = userMapper.getByOpenid(openid);
        //如果是新用户则自动完成注册
     if(user == null){
        user = User.builder()
                .openid(openid)
                .createTime(LocalDateTime.now())
                .build();
     userMapper.insert(user);
    }
     return user;
 }
}
