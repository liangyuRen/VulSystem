package com.nju.backend.controller;

import com.nju.backend.config.RespBean;
import com.nju.backend.config.RespBeanEnum;
import com.nju.backend.service.company.CompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/company")
public class CompanyController {
    @Autowired
    CompanyService companyService;

    @PostMapping("/updateStrategy")
    public RespBean updateStrategy(@RequestParam("companyId") Integer companyId, @RequestParam("similarityThreshold") double similarityThreshold, @RequestParam("maxDetectNums") Integer maxDetectNums, @RequestParam("detect_strategy") String detectStrategy) {
        try {
            companyService.updateStrategy(companyId, similarityThreshold, maxDetectNums, detectStrategy);
            return RespBean.success("更新成功");
        } catch (Exception e) {
            return RespBean.error(RespBeanEnum.ERROR, e.getMessage());
        }
    }



}
