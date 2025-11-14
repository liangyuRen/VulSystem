#!/bin/bash

# 后端 API 完整测试脚本
# 使用方式: bash api_test.sh

BASE_URL="http://localhost:8081"
COMPANY_ID=1
PROJECT_ID=22

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 测试结果统计
PASSED=0
FAILED=0
TOTAL=0

# 测试函数
test_api() {
    local name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"

    TOTAL=$((TOTAL + 1))

    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}测试 #${TOTAL}: ${name}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    echo "请求方式: $method"
    echo "端点: $endpoint"

    local url="${BASE_URL}${endpoint}"
    local response

    if [ "$method" = "GET" ]; then
        echo "完整URL: $url"
        response=$(curl -s -w "\n%{http_code}" "$url")
    elif [ "$method" = "POST" ]; then
        echo "请求数据: $data"
        response=$(curl -s -w "\n%{http_code}" -X POST "$url" \
            -H "Content-Type: application/x-www-form-urlencoded" \
            -d "$data")
    fi

    # 分离 HTTP 代码和响应体
    http_code=$(echo "$response" | tail -1)
    response_body=$(echo "$response" | head -n -1)

    echo "HTTP 状态码: $http_code"
    echo "响应内容:"
    echo "$response_body" | python3 -m json.tool 2>/dev/null || echo "$response_body"
    echo ""

    # 判断是否成功
    if echo "$response_body" | grep -q '"code":200'; then
        echo -e "${GREEN}✅ 测试通过${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}❌ 测试失败${NC}"
        FAILED=$((FAILED + 1))
    fi
}

# 开始测试
echo -e "${YELLOW}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${YELLOW}║         后端 API 完整测试套件                           ║${NC}"
echo -e "${YELLOW}╚════════════════════════════════════════════════════════╝${NC}"

# ============ 用户管理测试 ============
echo -e "\n${YELLOW}■ 用户管理 API 测试${NC}"

test_api "用户登录" "GET" "/user/login?username=rly&password=rly"

test_api "获取用户信息" "GET" "/user/info?username=rly"

test_api "用户注册" "POST" "/user/register" \
    "username=testapi_$(date +%s)&email=testapi_$(date +%s)@test.com&password=TestPass123&phone=13800000001"

# ============ 项目管理测试 ============
echo -e "\n${YELLOW}■ 项目管理 API 测试${NC}"

test_api "获取项目列表" "GET" "/project/list?companyId=${COMPANY_ID}&page=1&size=10"

test_api "获取项目信息" "GET" "/project/info?id=${PROJECT_ID}"

test_api "获取项目漏洞" "GET" "/project/getVulnerabilities?id=${PROJECT_ID}"

test_api "获取项目统计" "GET" "/project/statistics?companyId=${COMPANY_ID}"

test_api "更新项目" "POST" "/project/update" \
    "id=${PROJECT_ID}&name=mall-updated&description=Updated&risk_threshold=60&filePath=D:/kuling/upload/test"

# ============ 漏洞报告测试 ============
echo -e "\n${YELLOW}■ 漏洞报告 API 测试${NC}"

test_api "获取漏洞报告列表 (第1页)" "GET" "/vulnerabilityReport/list?page=1&size=20"

test_api "获取漏洞报告列表 (第2页)" "GET" "/vulnerabilityReport/list?page=2&size=20"

test_api "搜索漏洞报告" "GET" "/vulnerabilityReport/search?keyword=sql&page=1&size=20"

# ============ 公司管理测试 ============
echo -e "\n${YELLOW}■ 公司管理 API 测试${NC}"

test_api "获取公司策略" "GET" "/company/getStrategy?companyId=${COMPANY_ID}"

# ============ 测试摘要 ============
echo -e "\n${YELLOW}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${YELLOW}║         测试摘要                                        ║${NC}"
echo -e "${YELLOW}╚════════════════════════════════════════════════════════╝${NC}"

echo -e "总测试数: ${TOTAL}"
echo -e "通过: ${GREEN}${PASSED}${NC}"
echo -e "失败: ${RED}${FAILED}${NC}"
echo -e "成功率: $(awk "BEGIN {printf \"%.1f%%\", (${PASSED}/${TOTAL})*100}")"

if [ $FAILED -eq 0 ]; then
    echo -e "\n${GREEN}✅ 所有测试通过!${NC}"
    exit 0
else
    echo -e "\n${RED}❌ 部分测试失败${NC}"
    exit 1
fi
