#!/bin/bash

# 测试真实项目扫描和解析

echo "=========================================="
echo "真实项目扫描和依赖解析测试报告"
echo "=========================================="
echo ""

# Flask和Spring Boot服务器地址
FLASK_URL="http://127.0.0.1:5000"
SPRINGBOOT_URL="http://localhost:8081"

# 测试项目路径 (根据用户提供，这些是存储在数据库中的真实项目)
# Python 项目
PYTHON_PROJECT_1="/path/to/python/project1"
PYTHON_PROJECT_2="/path/to/python/project2"

# PHP 项目
PHP_PROJECT_1="/path/to/php/project1"
PHP_PROJECT_2="/path/to/php/project2"

# Rust 项目
RUST_PROJECT_1="/path/to/rust/project1"
RUST_PROJECT_2="/path/to/rust/project2"

# JavaScript 项目
JS_PROJECT_1="/path/to/javascript/project1"
JS_PROJECT_2="/path/to/javascript/project2"

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# ==================== 测试函数 ====================

test_project_language_detection() {
    local project_path=$1
    local project_name=$2
    
    echo ""
    echo "==== 语言检测: $project_name ===="
    echo "路径: $project_path"
    
    # URL编码路径
    local encoded_path=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''$project_path'''))")
    local url="${FLASK_URL}/parse/get_primary_language?project_folder=${encoded_path}&use_optimized=true"
    
    echo "API调用: GET $url"
    echo ""
    
    local response=$(curl -s "$url" 2>/dev/null)
    echo "响应:"
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
    echo ""
}

test_project_parsing() {
    local project_path=$1
    local project_name=$2
    local language=$3
    local parser_endpoint=$4
    
    echo ""
    echo "==== 依赖解析: $project_name ($language) ===="
    echo "路径: $project_path"
    
    # URL编码路径
    local encoded_path=$(python3 -c "import urllib.parse; print(urllib.parse.quote('''$project_path'''))")
    local url="${FLASK_URL}/parse/${parser_endpoint}?project_folder=${encoded_path}"
    
    echo "API调用: GET $url"
    echo ""
    
    local response=$(curl -s "$url" 2>/dev/null)
    echo "响应 (前100行):"
    echo "$response" | python3 -m json.tool 2>/dev/null | head -100 || echo "$response" | head -100
    echo ""
}

# ==================== 执行测试 ====================

echo "注意: 以下测试需要实际的项目路径。请确保:"
echo "1. 项目存在于文件系统"
echo "2. Flask服务运行在 http://127.0.0.1:5000"
echo "3. Spring Boot服务运行在 http://localhost:8081"
echo ""

# 创建示例测试数据（使用项目根目录作为测试项目）
SAMPLE_PROJECT="${PWD}"

echo "=== 使用项目根目录作为测试样本 ==="
echo "项目路径: $SAMPLE_PROJECT"
echo ""

# 测试语言检测
test_project_language_detection "$SAMPLE_PROJECT" "VulSystem项目"

# 根据检测到的语言进行相应的解析
# 注意: 由于这是一个混合项目，可能包含多种语言

echo "完整项目扫描测试完成"
echo ""
echo "要针对真实项目进行测试，请提供:"
echo "1. Python项目的具体路径"
echo "2. PHP项目的具体路径"
echo "3. Rust项目的具体路径"
echo "4. JavaScript项目的具体路径"
