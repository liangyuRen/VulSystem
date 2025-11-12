#!/bin/bash

# Complete Registration and Login Test Script
# This tests the full flow: register a user, then login with that user

BASE_URL="http://localhost:8081"

echo "═══════════════════════════════════════════════════════════════"
echo "            完整注册-登录流程测试"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Generate unique test user
TIMESTAMP=$(date +%s%N | cut -b1-10)
TEST_USER="testuser_${TIMESTAMP}"
TEST_EMAIL="test_${TIMESTAMP}@test.com"
TEST_PASSWORD="TestPass123"
TEST_PHONE="13800000001"

echo "📝 测试账户信息"
echo "───────────────────────────────────────────────────────────────"
echo "用户名: $TEST_USER"
echo "邮箱: $TEST_EMAIL"
echo "密码: $TEST_PASSWORD"
echo "电话: $TEST_PHONE"
echo ""

# Step 1: Test Registration
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ 步骤 1: 注册新用户"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "发送请求:"
echo "  POST $BASE_URL/user/register"
echo "  参数: username, email, password, phone"
echo ""

REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/user/register" \
  -d "username=${TEST_USER}&email=${TEST_EMAIL}&password=${TEST_PASSWORD}&phone=${TEST_PHONE}")

echo "API 响应:"
echo "$REGISTER_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$REGISTER_RESPONSE"
echo ""

# Check registration result
REGISTER_SUCCESS=false
if echo "$REGISTER_RESPONSE" | grep -q '"code":200'; then
    echo "✅ 注册成功 (code: 200 - SUCCESS)"
    REGISTER_SUCCESS=true
else
    echo "❌ 注册失败"
    if echo "$REGISTER_RESPONSE" | grep -q "已存在"; then
        echo "原因: 邮箱或用户名已存在"
    elif echo "$REGISTER_RESPONSE" | grep -q "格式不正确"; then
        echo "原因: 输入格式不正确"
    else
        echo "响应: $REGISTER_RESPONSE"
    fi
fi
echo ""

if [ "$REGISTER_SUCCESS" = true ]; then
    # Step 2: Test Login
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "✓ 步骤 2: 使用新注册账户登录"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""

    echo "发送请求:"
    echo "  GET $BASE_URL/user/login?username=$TEST_USER&password=$TEST_PASSWORD"
    echo ""

    LOGIN_RESPONSE=$(curl -s "$BASE_URL/user/login?username=${TEST_USER}&password=${TEST_PASSWORD}")

    echo "API 响应:"
    echo "$LOGIN_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$LOGIN_RESPONSE"
    echo ""

    # Check login result - SUCCESS code is 200
    if echo "$LOGIN_RESPONSE" | grep -q '"code":200'; then
        echo "✅ 登录成功 (code: 200 - SUCCESS)"
        echo ""
        echo "🎉 完整注册-登录流程验证成功！"
        echo ""
        echo "系统工作状态："
        echo "  ✅ 用户注册功能正常"
        echo "  ✅ 用户登录功能正常"
        echo "  ✅ 后端 API 响应正确"
        echo ""

        # Extract and display user info
        if command -v python3 &> /dev/null; then
            echo "返回的用户信息:"
            echo "$LOGIN_RESPONSE" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if 'obj' in data and data['obj']:
        user = data['obj']
        print(f\"  用户名: {user.get('userName', 'N/A')}\")
        print(f\"  邮箱: {user.get('email', 'N/A')}\")
        print(f\"  电话: {user.get('phone', 'N/A')}\")
        print(f\"  角色: {user.get('role', 'N/A')}\")
except:
    pass
" 2>/dev/null || true
            echo ""
        fi

    else
        echo "❌ 登录失败"
        if echo "$LOGIN_RESPONSE" | grep -q "用户名或密码错误"; then
            echo "原因: 用户名或密码错误"
        elif echo "$LOGIN_RESPONSE" | grep -q "用户名不存在"; then
            echo "原因: 用户名不存在"
        else
            echo "响应: $LOGIN_RESPONSE"
        fi
        echo ""
        echo "⚠️  这表明注册和登录之间存在问题"
    fi
else
    echo "⚠️  无法继续登录测试，因为注册失败"
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
