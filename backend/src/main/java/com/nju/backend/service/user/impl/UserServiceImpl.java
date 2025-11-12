package com.nju.backend.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nju.backend.config.JwtUtil;
import com.nju.backend.config.vo.UserVO;
import com.nju.backend.repository.mapper.UserMapper;
import com.nju.backend.repository.po.User;
import com.nju.backend.service.user.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

@Component
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public UserVO login(String username, String password) {

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", username);
        User user = userMapper.selectOne(queryWrapper);

        if(user == null) {
            throw new RuntimeException("用户名不存在");
        }
        if(!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        return userVO;
    }

    @Override
    public void register(User user) {
        // 检查邮箱是否已存在
        QueryWrapper<User> emailQuery = new QueryWrapper<>();
        emailQuery.eq("email", user.getEmail());
        User existingUser = userMapper.selectOne(emailQuery);

        if (existingUser != null) {
            throw new RuntimeException("该邮箱已被注册，请使用其他邮箱");
        }

        // 检查用户名是否已存在
        QueryWrapper<User> usernameQuery = new QueryWrapper<>();
        usernameQuery.eq("user_name", user.getUserName());
        User existingUsername = userMapper.selectOne(usernameQuery);

        if (existingUsername != null) {
            throw new RuntimeException("该用户名已被注册，请使用其他用户名");
        }

        // 加密密码并插入
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        try {
            userMapper.insert(user);
        } catch (Exception e) {
            // 捕获数据库约束违反等异常
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                if (e.getMessage().contains("uk_email")) {
                    throw new RuntimeException("该邮箱已被注册，请使用其他邮箱");
                } else if (e.getMessage().contains("user_name")) {
                    throw new RuntimeException("该用户名已被注册，请使用其他用户名");
                } else {
                    throw new RuntimeException("注册信息重复，请检查输入");
                }
            }
            throw e;
        }
    }

    public UserVO getinfo(String userName) {

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_name", userName);
        User user = userMapper.selectOne(queryWrapper);

        if(user == null) {
            throw new RuntimeException("用户名不存在");
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

}
