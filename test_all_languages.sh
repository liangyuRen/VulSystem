#!/bin/bash

echo "=================================="
echo "测试所有语言项目的依赖解析"
echo "=================================="
echo ""

# 函数：触发项目解析
trigger_parse() {
    local project_id=$1
    local language=$2
    local project_name=$3

    echo "→ 触发 $project_name ($language) 解析..."
    response=$(curl -s -X POST "http://localhost:8081/project/reparse" \
        -d "projectId=$project_id" \
        -d "language=$language")

    if echo "$response" | grep -q "SUCCESS"; then
        echo "  ✓ 解析请求已提交"
        return 0
    else
        echo "  ✗ 解析请求失败: $response"
        return 1
    fi
}

# 函数：查询数据库中的依赖数量
check_dependencies() {
    local language=$1
    echo "→ 查询 $language 依赖数量..."
    count=$(mysql -u root -p15256785749rly kulin -se \
        "SELECT COUNT(*) FROM white_list WHERE language='$language' AND isdelete=0" 2>/dev/null)
    echo "  当前 $language 依赖数: $count"
}

echo "步骤 1: 查看当前依赖统计"
echo "=================================="
mysql -u root -p15256785749rly kulin -e \
    "SELECT language, COUNT(*) as count FROM white_list WHERE isdelete=0 GROUP BY language ORDER BY language" 2>/dev/null
echo ""

echo "步骤 2: 触发各语言项目解析"
echo "=================================="
echo ""

# Rust 项目 (ID: 31) - Cargo.toml 存在
trigger_parse 31 "rust" "Rust项目"
echo ""

# Go 项目 (ID: 29) - 检查是否有 go.mod
echo "→ 检查 Go 项目配置..."
if [ -f "D:/kuling/upload/93ece2b3-26a5-47dd-8b6c-2bce1b016d05/shadowsocks-go-master/go.mod" ]; then
    trigger_parse 29 "go" "Go项目"
else
    echo "  ⚠ Go 项目没有 go.mod 文件，跳过"
fi
echo ""

# JavaScript 项目 (ID: 27) - 检查是否有 package.json
echo "→ 检查 JavaScript 项目配置..."
if [ -f "D:/kuling/upload/52db129b-ca8a-400c-93ba-7bfd0f8dda0d/basecamp-javascript-main/package.json" ]; then
    trigger_parse 27 "javascript" "JavaScript项目"
else
    echo "  ⚠ JavaScript 项目没有 package.json 文件，跳过"
fi
echo ""

# Erlang 项目 (ID: 28) - rebar.config 存在但可能无依赖
echo "→ 尝试 Erlang 项目解析（可能无依赖）..."
trigger_parse 28 "erlang" "Erlang项目"
echo ""

echo "步骤 3: 等待解析完成（60秒）..."
echo "=================================="
for i in {60..1}; do
    echo -ne "  剩余时间: $i 秒\r"
    sleep 1
done
echo ""
echo ""

echo "步骤 4: 查看解析结果"
echo "=================================="
mysql -u root -p15256785749rly kulin -e \
    "SELECT language, COUNT(*) as count FROM white_list WHERE isdelete=0 GROUP BY language ORDER BY language" 2>/dev/null
echo ""

echo "步骤 5: 查看新增的依赖（最近20条）"
echo "=================================="
mysql -u root -p15256785749rly kulin -e \
    "SELECT id, name, language FROM white_list WHERE isdelete=0 ORDER BY id DESC LIMIT 20" 2>/dev/null
echo ""

echo "=================================="
echo "测试完成！"
echo "=================================="
