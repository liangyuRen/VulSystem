#!/bin/bash

# 创建管理员账户脚本
# 这个脚本通过 API 注册一个管理员账户
# 用户名: admin
# 密码: admin

BASE_URL="http://localhost:8081"
USERNAME="admin"
EMAIL="admin@vulsystem.local"
PASSWORD="admin"
PHONE="13800000000"

echo "════════════════════════════════════════════════════════════════"
echo "               创建管理员账户"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "用户信息:"
echo "  用户名: $USERNAME"
echo "  邮箱: $EMAIL"
echo "  密码: $PASSWORD"
echo "  电话: $PHONE"
echo ""

# 检查后端是否运行
echo "检查后端服务状态..."
if ! curl -s -f "${BASE_URL}/user/login" -d "username=test&password=test" >/dev/null 2>&1; then
    echo "⚠ 警告: 后端服务可能未启动"
    echo "   请确保后端运行在 ${BASE_URL}"
    echo "   然后重新运行此脚本"
    exit 1
fi
echo "✓ 后端服务正常运行"
echo ""

# 注册管理员账户
echo "正在注册管理员账户..."
RESPONSE=$(curl -s -X POST "${BASE_URL}/user/register" \
  -d "username=${USERNAME}&email=${EMAIL}&password=${PASSWORD}&phone=${PHONE}")

echo "API 响应:"
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"
echo ""

# 检查是否成功
if echo "$RESPONSE" | grep -q '"code":0' || echo "$RESPONSE" | grep -q '"ok":true'; then
    echo "✓ 管理员账户创建成功！"
    echo ""
    echo "现在您可以用以下凭证登录:"
    echo "  用户名: $USERNAME"
    echo "  密码: $PASSWORD"
    echo ""

    # 测试登录
    echo "测试登录..."
    LOGIN_RESPONSE=$(curl -s -X GET "${BASE_URL}/user/login" \
      -d "username=${USERNAME}&password=${PASSWORD}")

    if echo "$LOGIN_RESPONSE" | grep -q '"code":0' || echo "$LOGIN_RESPONSE" | grep -q '"ok":true'; then
        echo "✓ 登录测试成功！"
        echo ""
        echo "登录信息:"
        echo "$LOGIN_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$LOGIN_RESPONSE"
    else
        echo "⚠ 登录失败，请检查"
        echo "响应: $LOGIN_RESPONSE"
    fi
else
    echo "✗ 创建失败"
    echo "错误信息可能是:"

    if echo "$RESPONSE" | grep -q "已被注册"; then
        echo "  - 该邮箱或用户名已被注册，请先删除旧账户"
    elif echo "$RESPONSE" | grep -q "格式"; then
        echo "  - 输入格式不正确"
    fi

    exit 1
fi

echo ""
echo "════════════════════════════════════════════════════════════════"
