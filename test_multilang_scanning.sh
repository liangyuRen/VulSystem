#!/bin/bash

# 多语言项目扫描测试脚本
# 演示如何使用新的多语言扫描API

echo "=========================================="
echo "多语言项目扫描系统 - 演示测试"
echo "=========================================="
echo ""

# 配置
SPRINGBOOT_URL="http://localhost:8081"
FLASK_URL="http://127.0.0.1:5000"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# ==================== 工具函数 ====================

print_section() {
    echo ""
    echo -e "${BLUE}========== $1 ==========${NC}"
    echo ""
}

test_flask_connection() {
    print_section "Flask服务器连接测试"

    echo "检测Flask服务器状态..."
    response=$(curl -s http://127.0.0.1:5000/vulnerabilities/test 2>/dev/null)

    if echo "$response" | grep -q "OK\|running"; then
        echo -e "${GREEN}✓ Flask服务器 运行正常${NC}"
        return 0
    else
        echo -e "${RED}✗ Flask服务器 未响应${NC}"
        return 1
    fi
}

test_springboot_connection() {
    print_section "Spring Boot服务器连接测试"

    echo "检测Spring Boot服务器状态..."
    response=$(curl -s -w "%{http_code}" http://localhost:8081/project/whitelist/1 2>/dev/null)

    if [[ $response == *"200"* ]] || [[ $response == *"404"* ]]; then
        echo -e "${GREEN}✓ Spring Boot服务器 运行正常${NC}"
        return 0
    else
        echo -e "${RED}✗ Spring Boot服务器 未响应${NC}"
        return 1
    fi
}

test_python_project() {
    print_section "Python项目扫描演示"

    local project_path="${1:-/path/to/python/project}"

    echo "项目路径: $project_path"
    echo "项目ID: 1"
    echo ""
    echo "发送扫描请求..."
    echo ""

    # 构造请求
    curl -s -X POST "$SPRINGBOOT_URL/project/scan" \
      -H "Content-Type: application/json" \
      -d "{
        \"projectPath\": \"$project_path\",
        \"projectId\": 1
      }" | python3 -m json.tool 2>/dev/null || echo "请求失败"
}

test_php_project() {
    print_section "PHP项目扫描演示"

    local project_path="${1:-/path/to/php/project}"

    echo "项目路径: $project_path"
    echo "项目ID: 2"
    echo ""
    echo "发送扫描请求..."
    echo ""

    curl -s -X POST "$SPRINGBOOT_URL/project/scan" \
      -H "Content-Type: application/json" \
      -d "{
        \"projectPath\": \"$project_path\",
        \"projectId\": 2
      }" | python3 -m json.tool 2>/dev/null || echo "请求失败"
}

test_javascript_project() {
    print_section "JavaScript项目扫描演示"

    local project_path="${1:-/path/to/javascript/project}"

    echo "项目路径: $project_path"
    echo "项目ID: 3"
    echo ""
    echo "发送扫描请求..."
    echo ""

    curl -s -X POST "$SPRINGBOOT_URL/project/scan" \
      -H "Content-Type: application/json" \
      -d "{
        \"projectPath\": \"$project_path\",
        \"projectId\": 3
      }" | python3 -m json.tool 2>/dev/null || echo "请求失败"
}

test_rust_project() {
    print_section "Rust项目扫描演示"

    local project_path="${1:-/path/to/rust/project}"

    echo "项目路径: $project_path"
    echo "项目ID: 4"
    echo ""
    echo "发送扫描请求..."
    echo ""

    curl -s -X POST "$SPRINGBOOT_URL/project/scan" \
      -H "Content-Type: application/json" \
      -d "{
        \"projectPath\": \"$project_path\",
        \"projectId\": 4
      }" | python3 -m json.tool 2>/dev/null || echo "请求失败"
}

test_get_whitelist() {
    print_section "获取White-list演示"

    local project_id=${1:-1}

    echo "查询项目 $project_id 的所有依赖..."
    echo ""

    curl -s "$SPRINGBOOT_URL/project/whitelist/$project_id" | python3 -m json.tool 2>/dev/null || echo "请求失败"
}

test_get_whitelist_by_language() {
    print_section "按语言查询White-list演示"

    local project_id=${1:-1}
    local language=${2:-python}

    echo "查询项目 $project_id 的 $language 依赖..."
    echo ""

    curl -s "$SPRINGBOOT_URL/project/whitelist/$project_id/$language" | python3 -m json.tool 2>/dev/null || echo "请求失败"
}

# ==================== 显示API参考 ====================

show_api_reference() {
    print_section "API快速参考"

    cat << 'EOF'
【1】扫描项目
POST /project/scan
Content-Type: application/json

{
  "projectPath": "/path/to/project",
  "projectId": 1
}

成功响应:
{
  "code": 200,
  "message": "Successfully scanned and saved X dependencies to white-list",
  "success": true,
  "data": {
    "projectPath": "...",
    "projectId": 1,
    "detectedLanguage": "python",
    "dependencyCount": 25,
    "savedCount": 25,
    "dependencies": [...]
  }
}

---

【2】获取所有依赖
GET /project/whitelist/{projectId}

示例: GET /project/whitelist/1

响应:
{
  "code": 200,
  "message": "Success",
  "data": [
    {
      "id": 1,
      "projectId": 1,
      "componentName": "requests",
      "componentVersion": "2.28.0",
      "language": "python",
      "packageManager": "pip",
      "status": "APPROVED",
      "createdTime": "2025-11-13 15:00:00"
    },
    ...
  ]
}

---

【3】按语言查询依赖
GET /project/whitelist/{projectId}/{language}

示例:
  GET /project/whitelist/1/python
  GET /project/whitelist/2/php
  GET /project/whitelist/3/javascript

响应: 与【2】相同，但仅包含指定语言的依赖

EOF
}

# ==================== 主程序 ====================

main() {
    echo "注意: 以下演示需要:"
    echo "1. Flask服务运行在 http://127.0.0.1:5000"
    echo "2. Spring Boot服务运行在 http://localhost:8081"
    echo "3. 实际的项目路径"
    echo ""

    # 测试连接
    if ! test_flask_connection; then
        echo -e "${YELLOW}警告: Flask服务未运行，某些测试可能失败${NC}"
    fi

    if ! test_springboot_connection; then
        echo -e "${YELLOW}警告: Spring Boot服务未运行，某些测试可能失败${NC}"
    fi

    # 显示API参考
    show_api_reference

    print_section "演示命令"

    cat << 'EOF'
要运行完整演示，请在提供实际项目路径后执行以下命令:

1. 扫描Python项目:
   test_python_project "/path/to/python/project"

2. 扫描PHP项目:
   test_php_project "/path/to/php/project"

3. 扫描JavaScript项目:
   test_javascript_project "/path/to/javascript/project"

4. 扫描Rust项目:
   test_rust_project "/path/to/rust/project"

5. 获取项目1的所有依赖:
   test_get_whitelist 1

6. 获取项目1的Python依赖:
   test_get_whitelist_by_language 1 python

7. 获取项目2的PHP依赖:
   test_get_whitelist_by_language 2 php

---

使用 curl 直接调用示例:

curl -X POST http://localhost:8081/project/scan \
  -H "Content-Type: application/json" \
  -d '{
    "projectPath": "/path/to/python/project",
    "projectId": 1
  }'

curl http://localhost:8081/project/whitelist/1

curl http://localhost:8081/project/whitelist/1/python

EOF
}

# 执行主程序
main
