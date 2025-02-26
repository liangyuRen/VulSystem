package com.nju.backend.service.company;

import com.nju.backend.config.vo.CompanyVO;

public interface CompanyService {
    void updateStrategy(Integer companyId, Double similarityThreshold, Integer maxDetectNums, String detectStrategy);

    CompanyVO getStrategy(Integer companyId);
}
