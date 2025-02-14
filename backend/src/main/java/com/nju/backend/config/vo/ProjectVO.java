package com.nju.backend.config.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Integer id;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 项目描述
     */
    private String projectDescription;

    /**
     * 项目使用的语言
     */
    private String language;

    /**
     * 高风险风险阈值，"0"表示高风险风险阈值
     */
    private Integer riskThreshold;
    
    /**
     *高风险漏洞数量 
     */
    private Integer highRiskNum;
    
    /**
     *低风险漏洞数量 
     */
    private Integer lowRiskNum;
    
    /**
     *中风险漏洞数量 
     */
    private Integer midRiskNum;
    
    /**
     *最后扫描时间
     */
    private String lastScanTime;

}
