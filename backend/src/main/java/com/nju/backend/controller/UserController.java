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
    public RespBean login(@RequestParam("username") String username,
                          @RequestParam("password") String password) {
        try {
            return RespBean.success(userService.login(username, password));
        }catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR,"用户名或密码错误");
        }
    }

    @PostMapping("/register")
    public RespBean register(
            @RequestParam("username") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
    @RequestParam("phone") String phone){
        try {
            // 验证输入参数
            if (name == null || name.trim().isEmpty()) {
                return RespBean.error(RespBeanEnum.ERROR, "用户名不能为空");
            }
            if (email == null || email.trim().isEmpty()) {
                return RespBean.error(RespBeanEnum.ERROR, "邮箱不能为空");
            }
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return RespBean.error(RespBeanEnum.ERROR, "邮箱格式不正确");
            }
            if (password == null || password.trim().isEmpty()) {
                return RespBean.error(RespBeanEnum.ERROR, "密码不能为空");
            }
            if (password.length() < 6) {
                return RespBean.error(RespBeanEnum.ERROR, "密码长度至少为6个字符");
            }
            if (phone == null || phone.trim().isEmpty()) {
                return RespBean.error(RespBeanEnum.ERROR, "电话号码不能为空");
            }

            User user = new User();
            user.setPassword(password);
            user.setCompanyName("company");
            user.setUserName(name);
            user.setEmail(email);
            user.setPhone(phone);
            user.setRole("user");
            user.setIsVip(0);
            user.setCompanyId(1);
            user.setActivationTime(Date.valueOf(LocalDate.now()));
            user.setTeam("noteam");
            user.setIsdelete(0);
            user.setIsvalid(0);
            user.setConfirmCode(password);

            userService.register(user);
            return RespBean.success("注册成功，请使用用户名和密码登录");
        } catch (RuntimeException e) {
            // 捕获业务异常，返回明确的错误信息
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        } catch (Exception e) {
            // 捕获其他异常
            String errorMsg = "注册失败";
            if (e.getMessage() != null) {
                if (e.getMessage().contains("Duplicate entry")) {
                    errorMsg = "注册信息已存在，请检查邮箱或用户名";
                } else if (e.getMessage().contains("Constraint")) {
                    errorMsg = "注册信息违反约束条件，请检查输入";
                }
            }
            return RespBean.error(RespBeanEnum.ERROR, errorMsg);
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
