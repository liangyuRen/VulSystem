-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建漏洞报告表
CREATE TABLE IF NOT EXISTS `vulnerability_report` (
                                                      `id` INT(11) NOT NULL AUTO_INCREMENT COMMENT '漏洞ID',
    `vulnerability_name` VARCHAR(255) NOT NULL COMMENT '漏洞名称',
    `disclosure_time` DATETIME NOT NULL COMMENT '披露时间',
    `risk_level` VARCHAR(50) NOT NULL COMMENT '风险等级（如：低、中、高、严重）',
    `reference_link` VARCHAR(255) DEFAULT NULL COMMENT '参考链接',
    `affects_whitelist` TINYINT(1) DEFAULT 0 COMMENT '是否影响了某公司白名单（1：是，0：否）',
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='漏洞报告表';