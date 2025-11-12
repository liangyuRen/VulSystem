package com.nju.backend.config.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO {

    private String userName;

    private String companyName;

    private String email;

    private boolean isVip;

    private String phone;

    private String role;

    private String team;

    private String token;  // JWT token

}
