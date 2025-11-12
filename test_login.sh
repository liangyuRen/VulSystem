#!/bin/bash

# 用户登录测试脚本（支持手动指定用户名和密码）
# 使用方式:
#   ./test_login.sh username password
#   或
#   ./test_login.sh  (交互模式)

BASE_URL="http://localhost:8081"

echo "════════════════════════════════════════════════════════════════"
echo "               用户登录测试"
echo "════════════════════════════════════════════════════════════════"
echo ""

# 检查是否提供了参数
if [ $# -eq 2 ]; then
    USERNAME="$1"
    PASSWORD="$2"
    INTERACTIVE=false
elif [ $# -eq 0 ]; then
    # 交互模式
    INTERACTIVE=true
else
    echo "使用方式:"
    echo "  $0 <username> <password>"
    echo "  $0  (进入交互模式)"
    echo ""
    exit 1
fi

# 交互模式
if [ "$INTERACTIVE" = true ]; then
    echo "📝 请输入登录信息"
    echo ""
    read -p "用户名: " USERNAME
    read -sp "密码: " PASSWORD
    echo ""
    echo ""
fi

echo "准备登录"
echo "─────────────────────────────────────────────────────────────"
echo "API 端点: $BASE_URL/user/login"
echo "用户名: $USERNAME"
echo "密码: $(printf '*%.0s' {1..${#PASSWORD}})"  # 用星号显示密码长度
echo ""

# 发送登录请求
echo "发送请求..."
RESPONSE=$(curl -s "$BASE_URL/user/login?username=${USERNAME}&password=${PASSWORD}")

echo ""
echo "API 响应:"
echo "─────────────────────────────────────────────────────────────"

# 尝试格式化 JSON 输出
if command -v python3 &> /dev/null; then
    if echo "$RESPONSE" | python3 -m json.tool 2>/dev/null; then
        :
    else
        echo "$RESPONSE"
    fi
elif command -v jq &> /dev/null; then
    echo "$RESPONSE" | jq . 2>/dev/null || echo "$RESPONSE"
else
    echo "$RESPONSE"
fi

echo ""
echo "─────────────────────────────────────────────────────────────"

# 检查响应
if echo "$RESPONSE" | grep -q '"code":200'; then
    echo "✅ 登录成功！"
    echo ""
    echo "返回的用户信息:"

    # 提取并显示用户信息
    if command -v python3 &> /dev/null; then
        echo "$RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if 'obj' in data and data['obj']:
        user = data['obj']
        print(f\"  用户名: {user.get('userName', 'N/A')}\")
        print(f\"  邮箱: {user.get('email', 'N/A')}\")
        print(f\"  电话: {user.get('phone', 'N/A')}\")
        print(f\"  角色: {user.get('role', 'N/A')}\")
        print(f\"  公司: {user.get('companyName', 'N/A')}\")
except:
    pass
" 2>/dev/null || true
    fi

    exit 0

elif echo "$RESPONSE" | grep -q '"code":0'; then
    echo "✅ 登录成功！"
    exit 0

elif echo "$RESPONSE" | grep -q "用户名或密码错误"; then
    echo "❌ 登录失败"
    echo "原因: 用户名或密码错误"
    exit 1

elif echo "$RESPONSE" | grep -q "用户名不存在"; then
    echo "❌ 登录失败"
    echo "原因: 用户名不存在"
    exit 1

else
    echo "❌ 登录失败"
    echo "原因: 未知错误"
    exit 1
fi
