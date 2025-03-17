package com.nju.backend;

import com.nju.backend.config.vo.UserVO;
import com.nju.backend.repository.po.User;
import com.nju.backend.service.user.UserService;
import com.nju.backend.service.user.impl.UserServiceImpl;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Date;
import java.time.LocalDate;

@SpringBootTest
public class UserTest {
    @Autowired
    private UserServiceImpl userService;
    @Test
    public void test() {
        User user = new User();
        user.setId(1);
        user.setCompanyId(0);
        user.setEmail("test@test.com");
        user.setUserName("testUser");
        user.setPhone("18888888888");
        user.setPassword("123456");
        user.setCompanyName("test");
        user.setActivationTime(Date.valueOf(LocalDate.now()));
        user.setIsVip(0);
        user.setTeam("team");
        user.setRole("role");
        user.setIsdelete(0);
        user.setIsvalid(0);
        user.setConfirmCode("123456");
        userService.register(user);
    }

    @Test
    public void loginTest() {
        String companyName = "test";
        String password = "123456";
        UserVO userVO = userService.login(companyName, password);
        System.out.println(userVO);
    }
}
