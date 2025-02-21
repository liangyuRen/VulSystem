package com.nju.backend.service.company.Impl;

import com.nju.backend.repository.mapper.CompanyMapper;
import com.nju.backend.repository.po.Company;
import com.nju.backend.service.company.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CompanyServiceImpl implements CompanyService {
    @Autowired
    CompanyMapper companyMapper;

    @Override
    public void updateStrategy(Integer companyId, Double similarityThreshold, Integer maxDetectNums, String detectStrategy) {
        Company company = companyMapper.selectById(companyId);
        if(company == null) {
            throw new RuntimeException("公司不存在");
        }
        company.setDetectStrategy(detectStrategy);
        company.setSimilarityThreshold(similarityThreshold);
        company.setMaxDetectNums(maxDetectNums);
        companyMapper.updateById(company);
    }
}
