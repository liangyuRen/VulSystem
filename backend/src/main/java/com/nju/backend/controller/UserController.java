package com.nju.backend.controller;

import com.nju.backend.config.RespBean;
import com.nju.backend.config.RespBeanEnum;
import com.nju.backend.service.user.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserServiceImpl userService;

    @RequestMapping("/login")
    public RespBean login(@RequestParam("companyName") String companyName,
                          @RequestParam("password") String password) {
        try {
            return RespBean.success(userService.login(companyName, password));
        }catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR,"用户名或密码错误");
        }
    }

}
