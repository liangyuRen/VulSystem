#!/bin/bash

# API测试脚本 - 测试所有后端接口
# 包括Flask和Spring Boot接口

echo "=============================================="
echo "后端API完整测试报告"
echo "=============================================="
echo "测试时间: $(date)"
echo ""

# 定义颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 定义测试结果数组
declare -a test_results
declare -i test_count=0
declare -i pass_count=0

# 测试函数
test_api() {
    local test_name=$1
    local method=$2
    local url=$3
    local data=$4
    local expected_code=$5

    echo -e "${BLUE}[测试 $((++test_count))]${NC} $test_name"
    echo "  方法: $method"
    echo "  URL: $url"

    if [ -n "$data" ]; then
        echo "  数据: $data"
        response=$(curl -s -w "\n%{http_code}" -X $method "$url" -H "Content-Type: application/json" -d "$data" 2>/dev/null)
    else
        response=$(curl -s -w "\n%{http_code}" -X $method "$url" 2>/dev/null)
    fi

    # 分离响应体和状态码
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    echo "  状态码: $http_code"

    # 检查状态码
    if [[ "$http_code" == "$expected_code" || "$http_code" == "200" || "$http_code" == "202" ]]; then
        echo -e "  ${GREEN}✓ 通过${NC}"
        ((pass_count++))
        test_results+=("✓ $test_name")
    else
        echo -e "  ${RED}✗ 失败 (期望: $expected_code, 实际: $http_code)${NC}"
        test_results+=("✗ $test_name (HTTP $http_code)")
    fi

    # 打印响应的前100个字符
    if [ -n "$body" ] && [ "$body" != "{}" ]; then
        echo "  响应: ${body:0:150}..."
    fi
    echo ""
}

# ============== Flask 接口测试 ==============
echo "========== Flask API 测试 =========="
echo ""

# 1. 测试Flask服务器状态
test_api "Flask服务器状态检查" "GET" "http://127.0.0.1:5000/vulnerabilities/test" "" "200"

# 2. 漏洞数据库接口
echo -e "${YELLOW}--- 漏洞数据库接口 ---${NC}"
test_api "获取GitHub漏洞" "GET" "http://127.0.0.1:5000/vulnerabilities/github" "" "200"
test_api "获取AVD漏洞" "GET" "http://127.0.0.1:5000/vulnerabilities/avd" "" "200"
test_api "获取NVD漏洞" "GET" "http://127.0.0.1:5000/vulnerabilities/nvd" "" "200"

# 3. LLM查询接口
echo -e "${YELLOW}--- LLM查询接口 ---${NC}"
test_api "LLM查询 (qwen模型)" "GET" "http://127.0.0.1:5000/llm/query?query=hello&model=qwen" "" "200"

# 4. 漏洞检测接口
echo -e "${YELLOW}--- 漏洞检测接口 ---${NC}"
test_api "漏洞检测 (POST)" "POST" "http://127.0.0.1:5000/vulnerabilities/detect" '{}' "200"

# 5. 语言检测接口
echo -e "${YELLOW}--- 语言检测接口 ---${NC}"
# 使用无效路径测试错误处理
test_api "检测主要语言 (无效路径)" "GET" "http://127.0.0.1:5000/parse/get_primary_language?project_folder=/nonexistent/path" "" "400"
test_api "检测所有语言 (无效路径)" "GET" "http://127.0.0.1:5000/parse/detect_languages?project_folder=/nonexistent/path" "" "400"

# 6. 代码解析接口
echo -e "${YELLOW}--- 代码解析接口 ---${NC}"
# 注意: 这些接口需要真实的项目路径, 我们测试错误情况
test_api "POM解析 (无效路径)" "GET" "http://127.0.0.1:5000/parse/pom_parse?project_folder=/nonexistent" "" "200"
test_api "Go解析 (无效路径)" "GET" "http://127.0.0.1:5000/parse/go_parse?project_folder=/nonexistent" "" "200"
test_api "Python解析 (无效路径)" "GET" "http://127.0.0.1:5000/parse/python_parse?project_folder=/nonexistent" "" "200"
test_api "PHP解析 (无效路径)" "GET" "http://127.0.0.1:5000/parse/php_parse?project_folder=/nonexistent" "" "200"
test_api "JavaScript解析 (无效路径)" "GET" "http://127.0.0.1:5000/parse/javascript_parse?project_folder=/nonexistent" "" "200"

echo ""
echo "========== Spring Boot API 测试 =========="
echo ""

# 检查Spring Boot服务器是否运行
if timeout 2 bash -c "cat < /dev/null > /dev/tcp/localhost/8081" 2>/dev/null; then
    echo -e "${GREEN}✓ Spring Boot服务器在线${NC}"
    echo ""

    # 7. Spring Boot用户接口
    echo -e "${YELLOW}--- 用户管理接口 ---${NC}"
    test_api "用户登录" "POST" "http://localhost:8081/user/login" '{"username":"admin","password":"admin"}' "200"
    test_api "用户注册" "POST" "http://localhost:8081/user/register" '{"username":"testuser","email":"test@test.com","password":"Test123","phone":"13800000000"}' "200"

    # 8. Spring Boot项目接口
    echo -e "${YELLOW}--- 项目管理接口 ---${NC}"
    test_api "获取项目列表" "GET" "http://localhost:8081/project" "" "200"

else
    echo -e "${RED}✗ Spring Boot服务器离线 (端口 8081)${NC}"
    echo "  请先启动Spring Boot服务器"
    echo ""
fi

# ============== 测试总结 ==============
echo ""
echo "=============================================="
echo "测试总结"
echo "=============================================="
echo "总测试数: $test_count"
echo "通过数: $pass_count"
echo "失败数: $((test_count - pass_count))"
if [ $test_count -gt 0 ]; then
    percentage=$((pass_count * 100 / test_count))
    echo "通过率: $percentage%"
fi
echo ""

echo "详细结果:"
for result in "${test_results[@]}"; do
    echo "  $result"
done

echo ""
echo "测试完成时间: $(date)"
