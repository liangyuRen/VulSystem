package com.nju.backend;

import com.nju.backend.config.vo.UserVO;
import com.nju.backend.repository.po.User;
import com.nju.backend.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Date;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserTest {
    @Autowired
    private UserServiceImpl userService;

    /**
     * 测试用户注册 - 使用唯一的邮箱和用户名
     */
    @Test
    public void testRegistration() {
        String timestamp = String.valueOf(System.currentTimeMillis());

        User user = new User();
        user.setCompanyId(1);
        user.setEmail("test_" + timestamp + "@test.com");           // 唯一邮箱
        user.setUserName("testUser_" + timestamp);                   // 唯一用户名
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

        // 执行注册
        assertDoesNotThrow(() -> userService.register(user));
        System.out.println("✅ 用户注册成功");
    }

    /**
     * 测试用户登录 - 使用正确的用户名
     */
    @Test
    public void testLogin() {
        String username = "testUser";  // 使用已存在的用户名
        String password = "123456";

        assertDoesNotThrow(() -> {
            UserVO userVO = userService.login(username, password);
            assertNotNull(userVO, "用户信息不应为空");
            System.out.println("✅ 用户登录成功: " + userVO);
        });
    }

    /**
     * 测试重复邮箱注册异常
     */
    @Test
    public void testDuplicateEmailException() {
        User user = new User();
        user.setCompanyId(1);
        user.setEmail("test@test.com");  // 已存在的邮箱
        user.setUserName("duplicateTest_" + System.currentTimeMillis());
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

        // 期望抛出异常
        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> userService.register(user));

        assertTrue(exception.getMessage().contains("该邮箱已被注册"),
            "应该返回邮箱已注册的错误消息");
        System.out.println("✅ 异常处理正确: " + exception.getMessage());
    }
}
