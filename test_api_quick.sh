#!/bin/bash

echo "========================================"
echo "漏洞检测API快速测试"
echo "========================================"

# 测试1: 检查服务器是否运行
echo ""
echo "测试1: 检查后端服务器状态..."
response=$(timeout 5 curl -s http://localhost:8081/actuator/health 2>&1)

if echo "$response" | grep -q "UP"; then
    echo "✓ 后端服务器正常运行"
else
    echo "✗ 后端服务器未运行或无响应"
    echo "  请先启动后端服务器"
    exit 1
fi

# 测试2: 检查Flask服务器
echo ""
echo "测试2: 检查Flask服务器状态..."
response=$(timeout 5 curl -s http://localhost:5000/parse/get_primary_language 2>&1)

if echo "$response" | grep -q "language"; then
    echo "✓ Flask服务器正常运行"
else
    echo "✗ Flask服务器未运行或无响应"
    echo "  请先启动Flask服务器"
    exit 1
fi

# 测试3: 检查漏洞报告数据
echo ""
echo "测试3: 检查漏洞报告数据..."
count=$(mysql -u root -p15256785749rly kulin -N -e "SELECT COUNT(*) FROM vulnerability_report;" 2>/dev/null)

if [ "$count" -gt 0 ]; then
    echo "✓ 找到 $count 个漏洞报告"
else
    echo "⚠ 没有找到漏洞报告"
    echo "  需要先运行XXL-JOB的漏洞爬取任务"
fi

# 测试4: 检查白名单数据
echo ""
echo "测试4: 检查白名单组件数据..."
count=$(mysql -u root -p15256785749rly kulin -N -e "SELECT COUNT(*) FROM white_list WHERE isdelete = 0;" 2>/dev/null)

if [ "$count" -gt 0 ]; then
    echo "✓ 找到 $count 个组件"
else
    echo "⚠ 没有找到组件数据"
    echo "  需要先上传项目并解析依赖"
fi

# 测试5: 调用检测API（指定公司和语言）
echo ""
echo "测试5: 调用漏洞检测API..."
echo "→ 检测公司1的Java组件..."

response=$(timeout 30 curl -s -X POST "http://localhost:8081/vulnerability/detect?companyId=1&language=java" 2>&1)

if echo "$response" | grep -q '"code":200'; then
    echo "✓ API调用成功"
    echo ""
    echo "返回结果:"
    echo "$response" | python -m json.tool 2>/dev/null || echo "$response"
else
    echo "✗ API调用失败"
    echo "响应: $response"
fi

# 测试6: 检查检测结果
echo ""
echo "测试6: 检查数据库中的检测结果..."
mysql -u root -p15256785749rly kulin -e "
SELECT language, COUNT(*) as count
FROM vulnerability
WHERE is_delete = 0
GROUP BY language
ORDER BY language;
" 2>/dev/null

echo ""
echo "========================================"
echo "测试完成!"
echo "========================================"
echo ""
echo "提示:"
echo "1. 如果需要全量检测，运行: curl -X POST http://localhost:8081/vulnerability/detect/all"
echo "2. 查看有风险的项目: python test_vulnerability_detection.py"
echo "3. 详细文档: VULNERABILITY_DETECTION_IMPLEMENTATION.md"
