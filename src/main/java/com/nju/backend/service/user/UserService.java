package com.nju.backend.service.user;


import com.nju.backend.config.vo.UserVO;
import com.nju.backend.repository.po.User;

import java.util.Map;

public interface UserService {

    UserVO login(String companyName, String password);

    void register(User user);

}
