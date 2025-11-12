package com.nju.backend.controller;

import com.nju.backend.config.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据库修复控制器 - 用于一次性修复编码问题
 */
@RestController
@RequestMapping("/admin/database")
public class DatabaseFixController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/fix-encoding")
    public RespBean fixEncoding() {
        try {
            StringBuilder log = new StringBuilder();

            // 1. 检查是否有乱码数据
            String checkSql = "SELECT COUNT(*) as count FROM vulnerability " +
                    "WHERE description IS NOT NULL AND (description LIKE '%æ%' OR description LIKE '%å%' OR description LIKE '%è%')";
            Integer vulnCount = jdbcTemplate.queryForObject(checkSql, Integer.class);
            log.append("发现 ").append(vulnCount).append(" 条vulnerability表乱码数据\n");

            String checkReportSql = "SELECT COUNT(*) as count FROM vulnerability_report " +
                    "WHERE description IS NOT NULL AND (description LIKE '%æ%' OR description LIKE '%å%' OR description LIKE '%è%')";
            Integer reportCount = jdbcTemplate.queryForObject(checkReportSql, Integer.class);
            log.append("发现 ").append(reportCount).append(" 条vulnerability_report表乱码数据\n");

            if (vulnCount == 0 && reportCount == 0) {
                return RespBean.success("没有发现需要修复的乱码数据");
            }

            // 2. 修复vulnerability表的description字段
            String fixVulnDesc = "UPDATE vulnerability " +
                    "SET description = CONVERT(CAST(CONVERT(description USING latin1) AS BINARY) USING utf8mb4) " +
                    "WHERE description IS NOT NULL AND (description LIKE '%æ%' OR description LIKE '%å%' OR description LIKE '%è%')";
            int updatedVulnDesc = jdbcTemplate.update(fixVulnDesc);
            log.append("修复了 ").append(updatedVulnDesc).append(" 条vulnerability.description记录\n");

            // 3. 修复vulnerability表的name字段
            String fixVulnName = "UPDATE vulnerability " +
                    "SET name = CONVERT(CAST(CONVERT(name USING latin1) AS BINARY) USING utf8mb4) " +
                    "WHERE name IS NOT NULL AND (name LIKE '%æ%' OR name LIKE '%å%' OR name LIKE '%è%')";
            int updatedVulnName = jdbcTemplate.update(fixVulnName);
            log.append("修复了 ").append(updatedVulnName).append(" 条vulnerability.name记录\n");

            // 4. 修复vulnerability_report表的description字段
            String fixReportDesc = "UPDATE vulnerability_report " +
                    "SET description = CONVERT(CAST(CONVERT(description USING latin1) AS BINARY) USING utf8mb4) " +
                    "WHERE description IS NOT NULL AND (description LIKE '%æ%' OR description LIKE '%å%' OR description LIKE '%è%')";
            int updatedReportDesc = jdbcTemplate.update(fixReportDesc);
            log.append("修复了 ").append(updatedReportDesc).append(" 条vulnerability_report.description记录\n");

            // 5. 修复vulnerability_report表的vulnerability_name字段
            String fixReportName = "UPDATE vulnerability_report " +
                    "SET vulnerability_name = CONVERT(CAST(CONVERT(vulnerability_name USING latin1) AS BINARY) USING utf8mb4) " +
                    "WHERE vulnerability_name IS NOT NULL AND (vulnerability_name LIKE '%æ%' OR vulnerability_name LIKE '%å%' OR vulnerability_name LIKE '%è%')";
            int updatedReportName = jdbcTemplate.update(fixReportName);
            log.append("修复了 ").append(updatedReportName).append(" 条vulnerability_report.vulnerability_name记录\n");

            // 6. 验证修复结果
            log.append("\n修复后验证:\n");
            String verifySql = "SELECT name, SUBSTRING(description, 1, 100) as description " +
                    "FROM vulnerability WHERE name = 'redis' LIMIT 1";
            List<Map<String, Object>> verifyResults = jdbcTemplate.queryForList(verifySql);
            if (!verifyResults.isEmpty()) {
                Map<String, Object> result = verifyResults.get(0);
                log.append("Redis漏洞描述: ").append(result.get("description")).append("\n");
            }

            return RespBean.success(log.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return new RespBean(500, "修复失败: " + e.getMessage(), null);
        }
    }
}
