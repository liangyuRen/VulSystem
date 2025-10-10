package com.nju.backend.controller;

import com.nju.backend.config.RespBean;
import com.nju.backend.config.RespBeanEnum;
import com.nju.backend.repository.po.User;
import com.nju.backend.service.user.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserServiceImpl userService;

    @GetMapping("/login")
    public RespBean login(@RequestParam("companyName") String companyName,
                          @RequestParam("password") String password) {
        try {
            return RespBean.success(userService.login(companyName, password));
        }catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR,"用户名或密码错误");
        }
    }
    //注册     username: info.username,
    //      email: info.email,
    //      password: info.password
    @PostMapping("/register")
    public RespBean register(
            @RequestParam("username") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        try {
            User user=new User();
            user.setPassword(password);
            user.setCompanyName(name);
            user.setUserName(name);
            user.setEmail(email);
            user.setPhone("0000000");
            user.setRole("user");
            user.setIsVip(0);
            user.setCompanyId(1);
            user.setActivationTime(Date.valueOf(LocalDate.now()));
            user.setTeam("noteam");
            user.setIsdelete(0);
            user.setIsvalid(0);
            user.setConfirmCode(password);
            userService.register(user);
            return RespBean.success();
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }

    @GetMapping("/info")
    public RespBean userinfo(@RequestParam("username") String userName) {
        try {
            return RespBean.success(userService.getinfo(userName));
        }catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR,"用户名或密码错误");
        }
    }

}
