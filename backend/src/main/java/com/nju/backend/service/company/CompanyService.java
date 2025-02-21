package com.nju.backend.service.company;

public interface CompanyService {
    void updateStrategy(Integer companyId, Double similarityThreshold, Integer maxDetectNums, String detectStrategy);
}
