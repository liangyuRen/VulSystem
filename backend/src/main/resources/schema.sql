use kulin;

CREATE TABLE IF NOT EXISTS `user`
(
    `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `email` VARCHAR(255) NOT NULL COMMENT '用户邮箱',
    `username` VARCHAR(255) NOT NULL COMMENT '用户姓名',
    `password` VARCHAR(255) NOT NULL COMMENT '用户密码',
    `confirm_code` VARCHAR(255) DEFAULT NULL COMMENT '邮箱注册唯一认证UUID',
    `activation_time` DATETIME DEFAULT NULL COMMENT '激活失效时间',
    `isvalid` TINYINT(1) DEFAULT 0 COMMENT '账号是否激活的标志（1：激活，0：未激活）',
    `isdelete` TINYINT(1) DEFAULT 0 COMMENT '软删除的标志（1：已删除，0：未删除）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_email` (`email`) COMMENT '邮箱唯一约束'
)
    ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `vulnerability_report`
(
    `id` int(11) NOT NULL AUTO_INCREMENT,
    `cve_id` VARCHAR(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `description` TEXT CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `vulnerability_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `disclosure_time` datetime NOT NULL,
    `riskLevel` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `referenceLink` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
    `affects_whitelist` int(11) NOT NULL,
    `isdelete`  int(11) NOT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE=InnoDB
    DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
    AUTO_INCREMENT=1
    ;

CREATE TABLE IF NOT EXISTS `vulnerability`
(
    `id`  int(11) NOT NULL AUTO_INCREMENT ,
    `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL ,
    `language`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL ,
    `time`  datetime NOT NULL ,
    `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL NOT NULL ,
    `riskLevel` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL ,
    `isaccept` INT(11) NOT NULL COMMENT '是否被采纳 0:用户未操作（默认状态） 1:采纳 2:不采纳',
    `isdelete`  int(11) NOT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE=InnoDB
    DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
    AUTO_INCREMENT=1
    ;

CREATE TABLE IF NOT EXISTS `company`
(
    `id`  int(11) NOT NULL AUTO_INCREMENT ,
    `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL ,
    `white_list` json NULL  COMMENT 'key:name value:projectid',
    `projectid` json NULL  COMMENT 'key:projectid value:language' ,
    `ismember` INT(11) NOT NULL COMMENT '是否是实验室合作企业',
    `isdelete`  int(11) NOT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE=InnoDB
    DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
    AUTO_INCREMENT=1
    ;

CREATE TABLE IF NOT EXISTS `project`
(
    `id`  int(11) NOT NULL AUTO_INCREMENT ,
    `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL ,
    `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL ,
    `language`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL ,
    `file`  varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '存的是路径',
    `roadmap_file` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '存的是路径' ,
    `risk_threshold` int(11) NOT NULL COMMENT '"0":高风险风险阈值' ,
    `isdelete`  int(11) NOT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE=InnoDB
    DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
    AUTO_INCREMENT=1
    ;

CREATE TABLE IF NOT EXISTS `project_vulnerability`
(
    `id`  int(11) NOT NULL AUTO_INCREMENT ,
    `project_id` int(11) NOT NULL ,
    `vulnerability_id` int(11) NOT NULL ,
    `isdelete`  int(11) NOT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE=InnoDB
    DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
    AUTO_INCREMENT=1
    ;

CREATE TABLE IF NOT EXISTS `vulnerability_report_vulnerability`
(
    `id`  int(11) NOT NULL AUTO_INCREMENT ,
    `vulnerability_report_id` int(11) NOT NULL ,
    `vulnerability_id` int(11) NOT NULL ,
    `isdelete`  int(11) NOT NULL,
    PRIMARY KEY (`id`)
)
    ENGINE=InnoDB
    DEFAULT CHARACTER SET=utf8 COLLATE=utf8_general_ci
    AUTO_INCREMENT=1
    ;