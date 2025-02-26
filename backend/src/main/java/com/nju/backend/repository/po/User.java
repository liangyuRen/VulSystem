package com.nju.backend.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户姓名
     **/
    @TableField("company_name")
    private String companyName;

    /**
     * 用户密码
     **/
    @TableField("password")
    private String password;

    /**
     * 邮箱注册唯一认证uuid
     **/
    @TableField("confirm_code")
    private String confirmCode;

    /**
     * 激活失效时间
     **/
    @TableField("activation_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "Asia/Shanghai")
    private Date activationTime;

    /**
     * 账号是否激活的标志
     **/
    @TableField("isvalid")
    private Integer isvalid;

    /**
     * 软删除的标志
     **/
    @TableField("isdelete")
    private Integer isdelete;
}
