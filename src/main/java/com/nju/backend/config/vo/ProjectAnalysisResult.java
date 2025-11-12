package com.nju.backend.config.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 项目分析结果VO类
 * 用于返回项目的分析状态和结果信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAnalysisResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目ID
     */
    private Integer projectId;

    /**
     * 检测到的项目语言
     * java/javascript/python/php/go/rust/ruby/erlang/detecting/unknown
     */
    private String language;

    /**
     * 检测到的组件数量
     */
    private Integer componentCount;

    /**
     * 检测到的漏洞数量
     */
    private Integer vulnerabilityCount;

    /**
     * 风险等级
     * 暂无风险/低风险/高风险
     */
    private String riskLevel;

    /**
     * 项目分析状态
     * pending(待分析)/analyzing(分析中)/completed(已完成)/failed(分析失败)
     */
    private String status;

    /**
     * 分析结果消息或错误描述
     */
    private String message;

    /**
     * 最后分析时间(时间戳)
     */
    private Long lastAnalysisTime;
}
