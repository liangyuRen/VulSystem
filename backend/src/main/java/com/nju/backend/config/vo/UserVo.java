package com.nju.backend.config.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVo {
    private static final long serialVersionUID = 1L;

    private Integer id;

    /**
     * 用户邮箱
     **/
    private String email;

    /**
     * 用户姓名
     **/
    private String username;

    /**
     * 用户密码
     **/
    private String password;

    /**
     * 用户修改新密码
     **/
    private String newPassword;

    /**
     * 用户验证密码
     **/
    private String confirmedPassword;

    /**
     * 找回密码验证码
     **/
    private String code;

    /**
     * 邮箱注册唯一认证confirmCode
     **/
    private String confirmCode;


    /**
     * 激活截止时间
     **/
    private Date activationTime;

    /**
     * 账号是否激活的标志
     **/
    private Integer isvalid;
}
