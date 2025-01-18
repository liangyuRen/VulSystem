package com.nju.backend.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/*
 * 公共返回对象的枚举
 * */
@Getter
@ToString
@AllArgsConstructor
public enum RespBeanEnum {
    SUCCESS(200,"SUCCESS"),
    ERROR(500,"服务端异常")
    ;
    private final int code;
    private final String message;

}
