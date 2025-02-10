package com.nju.backend.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("project")
public class Project implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 项目名称
     */
    @TableField("name")
    private String projectName;

    /**
     * 项目描述
     */
    @TableField("description")
    private String projectDescription;

    /**
     * 项目使用的语言
     */
    @TableField("language")
    private String language;

    /**
     * 项目文件地址
     */
    @TableField("file")
    private String file;

    /**
     * 项目路线图文件地址
     */
    @TableField("roadmap_file")
    private String roadmapFile;

    /**
     * 高风险风险阈值，"0"表示高风险风险阈值
     */
    @TableField("risk_threshold")
    private Integer riskThreshold;

    /**
     * 项目涉及的漏洞信息，JSON格式，例如：{"vulnerabilityId":""}
     */
    @TableField("vulnerability")
    private String vulnerability;

    /**
     * 软删除标志，0：未删除，1：已删除
     */
    @TableField("isdelete")
    private Integer isDelete;
}
