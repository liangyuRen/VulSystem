#!/bin/bash

# 语言检测功能完整测试脚本
# 测试项目上传、语言检测、数据库存储的完整流程

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  语言检测功能完整测试${NC}"
echo -e "${BLUE}========================================${NC}"

# API 地址
API_BASE="http://localhost:8081"
COMPANY_ID=1  # 需要根据实际情况修改

# 测试临时目录
TEST_DIR="/tmp/language_detection_test"
mkdir -p "$TEST_DIR"

# ==================== 辅助函数 ====================

function test_upload_project() {
    local project_name=$1
    local zip_file=$2
    local expected_language=$3

    echo -e "\n${YELLOW}[测试] 上传项目: $project_name${NC}"
    echo "  文件: $zip_file"
    echo "  预期语言: $expected_language"

    if [ ! -f "$zip_file" ]; then
        echo -e "${RED}✗ 文件不存在: $zip_file${NC}"
        return 1
    fi

    # 调用上传 API
    response=$(curl -s -X POST \
        -F "file=@$zip_file" \
        -F "name=$project_name" \
        -F "description=Test project for $expected_language" \
        -F "companyId=$COMPANY_ID" \
        "$API_BASE/project/uploadProject")

    echo "  响应: $response"

    # 检查响应
    if echo "$response" | grep -q '"code":200'; then
        detected_language=$(echo "$response" | grep -o '"detectedLanguage":"[^"]*"' | cut -d'"' -f4)
        echo -e "${GREEN}✓ 上传成功${NC}"
        echo "  检测到语言: $detected_language"

        if [ "$detected_language" = "$expected_language" ]; then
            echo -e "${GREEN}✓ 语言检测正确${NC}"
            return 0
        else
            echo -e "${RED}✗ 语言检测不正确${NC}"
            echo "  期望: $expected_language, 实际: $detected_language"
            return 1
        fi
    else
        echo -e "${RED}✗ 上传失败${NC}"
        return 1
    fi
}

function prepare_java_project() {
    echo -e "\n${BLUE}[准备] Java 项目${NC}"
    local dir="$TEST_DIR/java-project"
    mkdir -p "$dir/src/main/java/com/example"

    # 创建 pom.xml
    cat > "$dir/pom.xml" << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test-project</artifactId>
    <version>1.0</version>
</project>
EOF

    # 创建 Java 文件
    cat > "$dir/src/main/java/com/example/Main.java" << 'EOF'
package com.example;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}
EOF

    # 打包
    cd "$TEST_DIR"
    zip -r "java-project.zip" "java-project" > /dev/null 2>&1
    echo -e "${GREEN}✓ Java 项目已准备: $TEST_DIR/java-project.zip${NC}"
}

function prepare_python_project() {
    echo -e "\n${BLUE}[准备] Python 项目${NC}"
    local dir="$TEST_DIR/python-project"
    mkdir -p "$dir"

    # 创建 requirements.txt
    cat > "$dir/requirements.txt" << 'EOF'
requests==2.28.1
numpy==1.23.0
flask==2.1.0
EOF

    # 创建 Python 文件
    cat > "$dir/main.py" << 'EOF'
#!/usr/bin/env python3

import requests

def main():
    print("Hello from Python")

if __name__ == "__main__":
    main()
EOF

    # 打包
    cd "$TEST_DIR"
    zip -r "python-project.zip" "python-project" > /dev/null 2>&1
    echo -e "${GREEN}✓ Python 项目已准备: $TEST_DIR/python-project.zip${NC}"
}

function prepare_go_project() {
    echo -e "\n${BLUE}[准备] Go 项目${NC}"
    local dir="$TEST_DIR/go-project"
    mkdir -p "$dir"

    # 创建 go.mod
    cat > "$dir/go.mod" << 'EOF'
module github.com/example/myapp

go 1.19

require (
    github.com/gorilla/mux v1.8.0
)
EOF

    # 创建 Go 文件
    cat > "$dir/main.go" << 'EOF'
package main

import (
    "fmt"
)

func main() {
    fmt.Println("Hello from Go")
}
EOF

    # 打包
    cd "$TEST_DIR"
    zip -r "go-project.zip" "go-project" > /dev/null 2>&1
    echo -e "${GREEN}✓ Go 项目已准备: $TEST_DIR/go-project.zip${NC}"
}

function prepare_c_project() {
    echo -e "\n${BLUE}[准备] C 项目${NC}"
    local dir="$TEST_DIR/c-project"
    mkdir -p "$dir"

    # 创建 CMakeLists.txt
    cat > "$dir/CMakeLists.txt" << 'EOF'
cmake_minimum_required(VERSION 3.10)
project(MyProject)
add_executable(myapp main.c)
EOF

    # 创建 C 文件
    cat > "$dir/main.c" << 'EOF'
#include <stdio.h>

int main() {
    printf("Hello from C\n");
    return 0;
}
EOF

    # 打包
    cd "$TEST_DIR"
    zip -r "c-project.zip" "c-project" > /dev/null 2>&1
    echo -e "${GREEN}✓ C 项目已准备: $TEST_DIR/c-project.zip${NC}"
}

function prepare_rust_project() {
    echo -e "\n${BLUE}[准备] Rust 项目${NC}"
    local dir="$TEST_DIR/rust-project"
    mkdir -p "$dir/src"

    # 创建 Cargo.toml
    cat > "$dir/Cargo.toml" << 'EOF'
[package]
name = "myapp"
version = "0.1.0"
edition = "2021"

[dependencies]
EOF

    # 创建 Rust 文件
    cat > "$dir/src/main.rs" << 'EOF'
fn main() {
    println!("Hello from Rust");
}
EOF

    # 打包
    cd "$TEST_DIR"
    zip -r "rust-project.zip" "rust-project" > /dev/null 2>&1
    echo -e "${GREEN}✓ Rust 项目已准备: $TEST_DIR/rust-project.zip${NC}"
}

function prepare_javascript_project() {
    echo -e "\n${BLUE}[准备] JavaScript 项目${NC}"
    local dir="$TEST_DIR/js-project"
    mkdir -p "$dir"

    # 创建 package.json
    cat > "$dir/package.json" << 'EOF'
{
  "name": "myapp",
  "version": "1.0.0",
  "description": "Test JavaScript project",
  "main": "index.js",
  "dependencies": {
    "express": "^4.18.0",
    "lodash": "^4.17.21"
  }
}
EOF

    # 创建 JavaScript 文件
    cat > "$dir/index.js" << 'EOF'
const express = require('express');
const app = express();

console.log('Hello from JavaScript');
EOF

    # 打包
    cd "$TEST_DIR"
    zip -r "js-project.zip" "js-project" > /dev/null 2>&1
    echo -e "${GREEN}✓ JavaScript 项目已准备: $TEST_DIR/js-project.zip${NC}"
}

function prepare_unknown_project() {
    echo -e "\n${BLUE}[准备] 未知类型项目${NC}"
    local dir="$TEST_DIR/unknown-project"
    mkdir -p "$dir"

    # 创建一些随机文件，不包含任何已知的项目特征
    cat > "$dir/README.txt" << 'EOF'
This is a random project
EOF

    cat > "$dir/data.txt" << 'EOF'
Some data here
EOF

    # 打包
    cd "$TEST_DIR"
    zip -r "unknown-project.zip" "unknown-project" > /dev/null 2>&1
    echo -e "${GREEN}✓ 未知项目已准备: $TEST_DIR/unknown-project.zip${NC}"
}

function check_database() {
    local project_name=$1
    local expected_language=$2

    echo -e "\n${YELLOW}[检查] 数据库中的项目记录${NC}"

    # 查询项目信息
    project_info=$(mysql -h localhost -u root -proot vul_system -e \
        "SELECT id, name, language FROM project WHERE name = '$project_name' LIMIT 1;" 2>/dev/null)

    if [ -z "$project_info" ]; then
        echo -e "${RED}✗ 数据库中找不到项目: $project_name${NC}"
        return 1
    fi

    echo "  项目信息: $project_info"

    # 检查语言是否正确
    if echo "$project_info" | grep -q "$expected_language"; then
        echo -e "${GREEN}✓ 数据库中的语言正确${NC}"
        return 0
    else
        echo -e "${RED}✗ 数据库中的语言不正确${NC}"
        return 1
    fi
}

# ==================== 主测试流程 ====================

echo -e "\n${BLUE}[步骤 1] 准备测试项目${NC}"
prepare_java_project
prepare_python_project
prepare_go_project
prepare_c_project
prepare_rust_project
prepare_javascript_project
prepare_unknown_project

echo -e "\n${BLUE}[步骤 2] 测试项目上传${NC}"

test_upload_project "test-java" "$TEST_DIR/java-project.zip" "java"
test_upload_project "test-python" "$TEST_DIR/python-project.zip" "python"
test_upload_project "test-go" "$TEST_DIR/go-project.zip" "go"
test_upload_project "test-c" "$TEST_DIR/c-project.zip" "c"
test_upload_project "test-rust" "$TEST_DIR/rust-project.zip" "rust"
test_upload_project "test-javascript" "$TEST_DIR/js-project.zip" "javascript"
test_upload_project "test-unknown" "$TEST_DIR/unknown-project.zip" "unknown"

echo -e "\n${BLUE}[步骤 3] 等待异步处理${NC}"
echo "等待 5 秒使异步任务完成..."
sleep 5

echo -e "\n${BLUE}[步骤 4] 检查数据库${NC}"
check_database "test-java" "java"
check_database "test-python" "python"
check_database "test-go" "go"
check_database "test-c" "c"
check_database "test-rust" "rust"
check_database "test-javascript" "javascript"
check_database "test-unknown" "unknown"

# ==================== 清理 ====================

echo -e "\n${BLUE}[清理] 删除测试文件${NC}"
rm -rf "$TEST_DIR"
echo -e "${GREEN}✓ 测试文件已清理${NC}"

echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}  测试完成${NC}"
echo -e "${BLUE}========================================${NC}"
