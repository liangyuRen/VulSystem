#!/bin/bash

# 语言检测和项目解析流程测试脚本

echo "========================================"
echo "VulSystem 语言检测和项目解析测试"
echo "========================================"
echo ""

# 数据库连接信息
DB_HOST="localhost"
DB_PORT="3306"
DB_USER="root"
DB_PASSWORD="15256785749rly"
DB_NAME="kulin"

# 测试项目
TEST_PROJECT_PATH="D:\kuling\upload\huaweicloud-sdk-java-dis"

echo "【第一步】查看项目结构"
echo "项目路径: $TEST_PROJECT_PATH"
ls -la "$TEST_PROJECT_PATH" | head -15
echo ""

echo "【第二步】检查 Project 表中的数据"
echo "查询所有项目及其语言字段..."
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
SELECT
    id,
    name,
    language,
    file,
    DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%S') as create_time
FROM project
WHERE isdelete = 0
LIMIT 10;"
echo ""

echo "【第三步】检查 WhiteList 表中的依赖数据"
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
SELECT
    COUNT(*) as total_dependencies,
    language,
    COUNT(DISTINCT file_path) as project_count
FROM white_list
WHERE isdelete = 0
GROUP BY language;"
echo ""

echo "【第四步】验证语言字段和白名单的对应关系"
mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASSWORD $DB_NAME -e "
SELECT
    p.id,
    p.name,
    p.language as project_language,
    COUNT(w.id) as dependency_count,
    GROUP_CONCAT(DISTINCT w.language) as white_list_languages
FROM project p
LEFT JOIN white_list w ON p.file = w.file_path
WHERE p.isdelete = 0
GROUP BY p.id, p.name, p.language
ORDER BY p.id DESC
LIMIT 10;"
echo ""

echo "========================================  测试完成 ========================================"
