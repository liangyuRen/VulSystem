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
@TableName("company")
public class Company implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 公司ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 公司名称
     */
    @TableField("name")
    private String name;

    /**
     * 公司白名单，JSON格式，例如：{"name": "projectId"}
     */
    @TableField("white_list")
    private String whiteList;

    /**
     * 公司项目ID，JSON格式，例如：{"projectId": "language"}
     */
    @TableField("projectid")
    private String projectId;

    /**
     *是否实验室合作伙伴
     */
    @TableField("ismember")
    private Integer isMember;

    /**
     * 漏洞检测策略
     */
    @TableField("detect_strategy")
    private String detectStrategy;

    /**
     * 相似度阈值
     */
    @TableField("similarity_threshold")
    private Double similarityThreshold;

    /**
     * 最大检测次数
     */
    @TableField("max_detect_nums")
    private Integer maxDetectNums;

    /**
     * 软删除标志，0：未删除，1：已删除
     */
    @TableField("isdelete")
    private Integer isDelete;
}
