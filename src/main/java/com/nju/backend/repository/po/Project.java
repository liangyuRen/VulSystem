package com.nju.backend.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nju.backend.config.vo.ProjectVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

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
    private String name;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 项目描述
     */
    @TableField("description")
    private String description;

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
     * 软删除标志，0：未删除，1：已删除
     */
    @TableField("isdelete")
    private Integer isDelete;

    /**
     * 项目分析状态
     * pending(待分析)/analyzing(分析中)/completed(已完成)/failed(分析失败)
     */
    @TableField("analysis_status")
    private String analysisStatus;

    /**
     * 分析结果消息或错误描述
     */
    @TableField("analysis_message")
    private String analysisMessage;

    /**
     * 检测到的组件数量
     */
    @TableField("component_count")
    private Integer componentCount;

    /**
     * 检测到的漏洞数量
     */
    @TableField("vulnerability_count")
    private Integer vulnerabilityCount;

    /**
     * 风险等级
     * 暂无风险/低风险/高风险
     */
    @TableField("risk_level")
    private String riskLevel;

    /**
     * 最后分析时间(时间戳)
     */
    @TableField("last_analysis_time")
    private Long lastAnalysisTime;

    public ProjectVO toVO() {
        ProjectVO projectVO = new ProjectVO();
        projectVO.setId(this.id);
        projectVO.setProjectName(this.name);
        projectVO.setCreateTime(this.createTime.toString());
        projectVO.setProjectDescription(this.description);
        projectVO.setLanguage(this.language);
        projectVO.setRiskThreshold(this.riskThreshold);
        return projectVO;
    }
}
