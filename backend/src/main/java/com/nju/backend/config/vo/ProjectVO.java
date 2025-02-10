package com.nju.backend.config.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectVO {
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
     * 项目描述
     */
    private String projectDescription;

    /**
     * 项目使用的语言
     */
    private String language;

    /**
     * 项目文件地址
     */
    private String file;

    /**
     * 项目路线图文件地址
     */
    private String roadmapFile;

    /**
     * 高风险风险阈值，"0"表示高风险风险阈值
     */
    private Integer riskThreshold;

    /**
     * 项目涉及的漏洞信息，JSON格式，例如：{"vulnerabilityId":""}
     */
    private String vulnerability;

    /**
     * 软删除标志，0：未删除，1：已删除
     */
    private Integer isDelete;
}
