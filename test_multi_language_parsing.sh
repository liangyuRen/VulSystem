#!/bin/bash

# 多语言项目依赖解析测试脚本
# 用于测试所有支持语言的解析功能

echo "========================================="
echo "   多语言依赖解析系统测试"
echo "========================================="
echo ""

# 配置
BACKEND_URL="http://localhost:8081"
FLASK_URL="http://localhost:5000"
PROJECT_ID=1  # 修改为你的测试项目ID

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 测试计数器
total_tests=0
passed_tests=0
failed_tests=0

# 测试函数
test_language() {
    local language=$1
    local test_name=$2

    echo ""
    echo "----------------------------------------"
    echo "测试 ${test_name} 项目解析"
    echo "----------------------------------------"

    total_tests=$((total_tests + 1))

    # 调用重新解析接口
    response=$(curl -s -X POST "${BACKEND_URL}/project/reparse" \
        -d "projectId=${PROJECT_ID}" \
        -d "language=${language}")

    # 检查响应
    if echo "$response" | grep -q '"code":200'; then
        echo -e "${GREEN}✓ ${test_name} 解析请求成功${NC}"
        echo "响应: $response"
        passed_tests=$((passed_tests + 1))
    else
        echo -e "${RED}✗ ${test_name} 解析请求失败${NC}"
        echo "响应: $response"
        failed_tests=$((failed_tests + 1))
    fi
}

# 检查Flask服务
echo "步骤 1: 检查Flask服务状态"
flask_status=$(curl -s -o /dev/null -w "%{http_code}" "${FLASK_URL}/vulnerabilities/test")
if [ "$flask_status" -eq 200 ]; then
    echo -e "${GREEN}✓ Flask服务正常运行 (Port 5000)${NC}"
else
    echo -e "${RED}✗ Flask服务未运行，请先启动Flask服务${NC}"
    echo "  启动命令: cd flask-service && python app.py"
    exit 1
fi

# 检查Spring Boot服务
echo ""
echo "步骤 2: 检查Spring Boot服务状态"
backend_status=$(curl -s -o /dev/null -w "%{http_code}" "${BACKEND_URL}/project/info?projectid=${PROJECT_ID}")
if [ "$backend_status" -eq 200 ]; then
    echo -e "${GREEN}✓ Spring Boot服务正常运行 (Port 8081)${NC}"
else
    echo -e "${RED}✗ Spring Boot服务未运行或项目ID不存在${NC}"
    echo "  请检查服务状态和项目ID: ${PROJECT_ID}"
    exit 1
fi

echo ""
echo "========================================="
echo "步骤 3: 测试各语言解析功能"
echo "========================================="

# 测试所有支持的语言
test_language "java" "Java"
test_language "python" "Python"
test_language "go" "Go"
test_language "rust" "Rust"
test_language "javascript" "JavaScript"
test_language "php" "PHP"
test_language "ruby" "Ruby"
test_language "erlang" "Erlang"
test_language "c" "C/C++"

echo ""
echo "========================================="
echo "步骤 4: 测试批量解析功能"
echo "========================================="

total_tests=$((total_tests + 1))

batch_response=$(curl -s -X POST "${BACKEND_URL}/project/reparse/multiple" \
    -d "projectId=${PROJECT_ID}" \
    -d "languages=java,python,go")

if echo "$batch_response" | grep -q '"code":200'; then
    echo -e "${GREEN}✓ 批量解析请求成功${NC}"
    echo "响应: $batch_response"
    passed_tests=$((passed_tests + 1))
else
    echo -e "${RED}✗ 批量解析请求失败${NC}"
    echo "响应: $batch_response"
    failed_tests=$((failed_tests + 1))
fi

echo ""
echo "========================================="
echo "步骤 5: 直接测试Flask端接口"
echo "========================================="

# 测试项目路径（需要修改为实际存在的项目路径）
TEST_PROJECT_PATH="C:/test/sample-project"

echo ""
echo "测试Flask Python解析接口..."
python_response=$(curl -s "${FLASK_URL}/parse/python_parse?project_folder=${TEST_PROJECT_PATH}" | head -c 200)
echo "Python解析响应: ${python_response}..."

echo ""
echo "测试Flask Go解析接口..."
go_response=$(curl -s "${FLASK_URL}/parse/go_parse?project_folder=${TEST_PROJECT_PATH}" | head -c 200)
echo "Go解析响应: ${go_response}..."

echo ""
echo "========================================="
echo "测试结果汇总"
echo "========================================="
echo "总测试数: ${total_tests}"
echo -e "${GREEN}通过: ${passed_tests}${NC}"
echo -e "${RED}失败: ${failed_tests}${NC}"

if [ $failed_tests -eq 0 ]; then
    echo -e "${GREEN}"
    echo "╔════════════════════════════════════╗"
    echo "║   所有测试通过！系统运行正常      ║"
    echo "╚════════════════════════════════════╝"
    echo -e "${NC}"
    exit 0
else
    echo -e "${RED}"
    echo "╔════════════════════════════════════╗"
    echo "║   部分测试失败，请检查错误日志    ║"
    echo "╚════════════════════════════════════╝"
    echo -e "${NC}"
    exit 1
fi
