package com.nju.backend.config.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyVO {
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
     * 软删除标志，0：未删除，1：已删除
     */
    private Integer isDelete;
}
