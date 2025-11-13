package com.nju.backend.repository.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("white_list")
public class WhiteList {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(exist = false)
    private Long projectId;  // 内存字段，不对应数据库列

    @TableField(exist = false)
    private String componentName;  // 内存字段，映射到 name

    @TableField(exist = false)
    private String componentVersion;  // 内存字段

    private String name;

    @TableField("file_path")
    private String filePath;

    private String description;

    private String language;

    @TableField(exist = false)
    private String packageManager;  // 内存字段

    @TableField(exist = false)
    private String status;  // 内存字段

    @TableField(exist = false)
    private String remark;  // 内存字段

    @TableField(exist = false)
    private Date createdTime;  // 内存字段

    private int isdelete;

    // ========== 完整的 Getter/Setter 方法 ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getComponentName() {
        return componentName != null ? componentName : name;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
        // 同时更新 name 字段以保持一致性
        if (componentName != null) {
            this.name = componentName;
        }
    }

    public String getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(String componentVersion) {
        this.componentVersion = componentVersion;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPackageManager() {
        return packageManager;
    }

    public void setPackageManager(String packageManager) {
        this.packageManager = packageManager;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public int getIsdelete() {
        return isdelete;
    }

    public void setIsdelete(int isdelete) {
        this.isdelete = isdelete;
    }

    @Override
    public String toString() {
        return "WhiteList{" +
                "id=" + id +
                ", projectId=" + projectId +
                ", componentName='" + componentName + '\'' +
                ", componentVersion='" + componentVersion + '\'' +
                ", name='" + name + '\'' +
                ", filePath='" + filePath + '\'' +
                ", description='" + description + '\'' +
                ", language='" + language + '\'' +
                ", packageManager='" + packageManager + '\'' +
                ", status='" + status + '\'' +
                ", remark='" + remark + '\'' +
                ", createdTime=" + createdTime +
                ", isdelete=" + isdelete +
                '}';
    }
}
