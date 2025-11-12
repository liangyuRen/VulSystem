package com.nju.backend.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("white_list")
public class WhiteList {
    private int id;

    @TableField("project_id")
    private Integer projectId;  // 新增: 项目ID

    private String name;

    @TableField("version")
    private String version;  // 新增: 组件版本

    @TableField("file_path")
    private String filePath;

    private String description;

    private String language;

    @TableField("package_manager")
    private String packageManager;  // 新增: 包管理器 (npm/pip/maven)

    @TableField("source_url")
    private String sourceUrl;  // 新增: 官方仓库URL

    @TableField("created_time")
    private Long createdTime;  // 新增: 创建时间

    private int isdelete;

}
