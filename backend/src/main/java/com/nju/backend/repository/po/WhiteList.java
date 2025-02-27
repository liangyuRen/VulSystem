package com.nju.backend.repository.po;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("white_list")
public class WhiteList {
    private int id;

    private String name;

    private String description;

    private String language;

    private int project_id;

    private int company_id;

    private int isdelete;

}
