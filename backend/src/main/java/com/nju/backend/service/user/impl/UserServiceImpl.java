package com.nju.backend.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nju.backend.config.JwtUtil;
import com.nju.backend.repository.mapper.UserMapper;
import com.nju.backend.repository.po.User;
import com.nju.backend.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Map<String,Integer> login(String companyName, String password) {
        String encodedPassword = passwordEncoder.encode(companyName);

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("company_name", companyName);
        User user = userMapper.selectOne(queryWrapper);

        if(user == null) {
            throw new RuntimeException("用户名不存在");
        }
        if(!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        Map<String,Integer> map = new HashMap<>();
        map.put("companyId", user.getId());
        map.put("companyName", user.getId());
        return map;
    }

}
