package com.nju.backend.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nju.backend.repository.po.Company;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CompanyMapper extends BaseMapper<Company> {
}
