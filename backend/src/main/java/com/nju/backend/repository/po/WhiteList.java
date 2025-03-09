package com.nju.backend.repository.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("white_list")
public class WhiteList {
    private int id;

    private String name;

    @TableField("file_path")
    private String filePath;

    private String description;

    private String language;

    private int company_id;

    private int isdelete;

}
