-- ==========================================
-- 修复所有相关表的字符集问题
-- ==========================================
-- 问题: 多个表使用 utf8mb3, 导致中文乱码
-- 解决: 转换所有表为 utf8mb4

USE kulin;

-- ==========================================
-- 1. vulnerability 表
-- ==========================================
ALTER TABLE vulnerability
CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE vulnerability
MODIFY COLUMN name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
MODIFY COLUMN language VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
MODIFY COLUMN description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
MODIFY COLUMN riskLevel VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
MODIFY COLUMN ref VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT '✓ vulnerability 表已转换为 utf8mb4' AS status;

-- ==========================================
-- 2. white_list 表
-- ==========================================
ALTER TABLE white_list
CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE white_list
MODIFY COLUMN file_path VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
MODIFY COLUMN language VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
MODIFY COLUMN description TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
MODIFY COLUMN name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

SELECT '✓ white_list 表已转换为 utf8mb4' AS status;

-- ==========================================
-- 3. project 表
-- ==========================================
ALTER TABLE project
CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE project
MODIFY COLUMN name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
MODIFY COLUMN description VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
MODIFY COLUMN language VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

SELECT '✓ project 表已转换为 utf8mb4' AS status;

-- ==========================================
-- 4. company 表（如果存在）
-- ==========================================
ALTER TABLE company
CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE company
MODIFY COLUMN name VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT '✓ company 表已转换为 utf8mb4' AS status;

-- ==========================================
-- 验证结果
-- ==========================================
SELECT
    TABLE_NAME,
    TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'kulin'
  AND TABLE_NAME IN ('vulnerability_report', 'vulnerability', 'white_list', 'project', 'company')
ORDER BY TABLE_NAME;

SELECT '==========================================' AS separator;
SELECT '所有表已成功转换为 utf8mb4!' AS result;
SELECT '==========================================' AS separator;
