package com.nju.backend.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nju.backend.repository.po.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<User> {

}

