package com.nju.backend.service.user.impl;

import com.nju.backend.repository.mapper.UserMapper;
import com.nju.backend.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;



}
