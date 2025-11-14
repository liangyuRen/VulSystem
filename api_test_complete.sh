#!/bin/bash

# API测试脚本 - 完整版本
# 测试所有后端接口

BASE_URL="http://localhost:8081"
TEST_RESULTS="api_test_results.txt"

echo "========================================" | tee "$TEST_RESULTS"
echo "VulSystem Backend API Testing Report"
echo "========================================" | tee -a "$TEST_RESULTS"
echo "Test Time: $(date)" | tee -a "$TEST_RESULTS"
echo "" | tee -a "$TEST_RESULTS"

# Test counter
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Helper function to test API
test_api() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_status=$5

    TOTAL_TESTS=$((TOTAL_TESTS + 1))

    echo "Test $TOTAL_TESTS: $name" | tee -a "$TEST_RESULTS"
    echo "  URL: $BASE_URL$endpoint" | tee -a "$TEST_RESULTS"
    echo "  Method: $method" | tee -a "$TEST_RESULTS"

    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    else
        if [ -n "$data" ]; then
            response=$(curl -s -w "\n%{http_code}" -X "$method" -d "$data" "$BASE_URL$endpoint")
        else
            response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint")
        fi
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    echo "  Status: $http_code" | tee -a "$TEST_RESULTS"
    echo "  Response: ${body:0:200}" | tee -a "$TEST_RESULTS"

    if [[ "$http_code" =~ ^[2-3] ]]; then
        echo "  Result: ✓ PASSED" | tee -a "$TEST_RESULTS"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo "  Result: ✗ FAILED" | tee -a "$TEST_RESULTS"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    echo "" | tee -a "$TEST_RESULTS"
}

# ===== USER ENDPOINTS =====
echo "=== USER ENDPOINTS ===" | tee -a "$TEST_RESULTS"
test_api "User Login" "GET" "/user/login?username=testuser&password=testpass" "" ""
test_api "User Register" "POST" "/user/register" "username=newuser123&email=newuser@test.com&password=TestPass123&phone=13800000001" ""
test_api "User Info" "GET" "/user/info?username=testuser" "" ""

# ===== COMPANY ENDPOINTS =====
echo "=== COMPANY ENDPOINTS ===" | tee -a "$TEST_RESULTS"
test_api "Get Company Strategy" "GET" "/company/getStrategy?companyId=1" "" ""
test_api "Update Company Strategy" "POST" "/company/updateStrategy" "companyId=1&similarityThreshold=0.8&maxDetectNums=5&detect_strategy=similarity" ""

# ===== PROJECT ENDPOINTS =====
echo "=== PROJECT ENDPOINTS ===" | tee -a "$TEST_RESULTS"
test_api "Get Project List" "GET" "/project/list?companyId=1&page=1&size=10" "" ""
test_api "Get Project Info" "GET" "/project/info?projectid=1" "" ""
test_api "Get Project Vulnerabilities" "GET" "/project/getVulnerabilities?id=1" "" ""
test_api "Get Project Statistics" "GET" "/project/statistics?companyId=1" "" ""

# ===== VULNERABILITY ENDPOINTS =====
echo "=== VULNERABILITY ENDPOINTS ===" | tee -a "$TEST_RESULTS"
test_api "Accept Vulnerability Suggestion" "GET" "/vulnerability/accept?vulnerabilityid=1&ifaccept=true" "" ""

# ===== VULNERABILITY REPORT ENDPOINTS =====
echo "=== VULNERABILITY REPORT ENDPOINTS ===" | tee -a "$TEST_RESULTS"
test_api "Get Vulnerability Report List" "GET" "/vulnerabilityReport/list?page=1&size=10" "" ""
test_api "Search Vulnerability Report" "GET" "/vulnerabilityReport/search?keyword=test" "" ""
test_api "Filter Vulnerability Report" "GET" "/vulnerabilityReport/filter?riskLevel=high" "" ""

# ===== SUMMARY =====
echo "========================================" | tee -a "$TEST_RESULTS"
echo "TEST SUMMARY" | tee -a "$TEST_RESULTS"
echo "========================================" | tee -a "$TEST_RESULTS"
echo "Total Tests: $TOTAL_TESTS" | tee -a "$TEST_RESULTS"
echo "Passed: $PASSED_TESTS" | tee -a "$TEST_RESULTS"
echo "Failed: $FAILED_TESTS" | tee -a "$TEST_RESULTS"
echo "Pass Rate: $(echo "scale=2; $PASSED_TESTS * 100 / $TOTAL_TESTS" | bc)%" | tee -a "$TEST_RESULTS"
echo "========================================" | tee -a "$TEST_RESULTS"

echo ""
echo "Test results saved to: $TEST_RESULTS"
