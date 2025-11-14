#!/bin/bash

echo "=========================================="
echo "快速API接口测试"
echo "=========================================="
echo ""

# Flask服务器基础测试
echo "[1] Flask服务器状态"
curl -s http://127.0.0.1:5000/vulnerabilities/test | python3 -m json.tool 2>/dev/null || echo "Failed to connect"
echo ""

# 漏洞数据库接口 - 简要测试
echo "[2] 获取GitHub漏洞接口响应状态"
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1:5000/vulnerabilities/github
echo ""

echo "[3] 获取AVD漏洞接口响应状态"
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1:5000/vulnerabilities/avd
echo ""

echo "[4] 获取NVD漏洞接口响应状态"
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://127.0.0.1:5000/vulnerabilities/nvd
echo ""

# 语言检测接口
echo "[5] 语言检测接口 (使用无效路径测试错误处理)"
curl -s http://127.0.0.1:5000/parse/get_primary_language?project_folder=/nonexistent | python3 -m json.tool 2>/dev/null | head -20
echo ""

# 修复建议异步接口
echo "[6] 漏洞修复建议接口 (异步)"
curl -s -X POST http://127.0.0.1:5000/llm/repair/suggestion \
  -F "vulnerability_name=XSS" \
  -F "vulnerability_desc=Cross-site Scripting" \
  -F "model=qwen" | python3 -m json.tool 2>/dev/null
echo ""

# Spring Boot接口测试
echo "[7] Spring Boot服务器状态"
timeout 2 curl -s http://localhost:8081/user/login \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}' | python3 -m json.tool 2>/dev/null || echo "Spring Boot server not responding"
