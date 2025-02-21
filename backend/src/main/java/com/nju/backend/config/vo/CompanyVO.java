package com.nju.backend.config.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 公司ID
     */
    private Integer id;

    /**
     * 公司名称
     */
    private String name;

    /**
     * 公司白名单，JSON格式，例如：{"name": "projectId"}
     */
    private String whiteList;

    /**
     * 公司项目ID，JSON格式，例如：{"projectId": "language"}
     */
    private String projectId;

    /**
     *是否实验室合作伙伴
     */
    private Integer isMember;

    /**
     * 软删除标志，0：未删除，1：已删除
     */
    private Integer isDelete;

    /**
     * 漏洞检测策略
     */
    private String detectStrategy;

    /**
     * 相似度阈值
     */
    private Double similarityThreshold;

    /**
     * 最大检测次数
     */
    private Integer maxDetectNums;

}
