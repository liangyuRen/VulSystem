package com.nju.backend.service.user;


import java.util.Map;

public interface UserService {

    Map<String,Integer> login(String companyName, String password);

}
