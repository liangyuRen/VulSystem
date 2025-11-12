#!/bin/bash

# 用户注册和登录测试脚本
# 用于测试修复后的用户注册和登录功能

BASE_URL="http://localhost:8080"
USER_NAME="testuser123"
EMAIL="testuser123@example.com"
PASSWORD="testpass123"
PHONE="13800138000"

echo "========================================="
echo "用户注册和登录测试"
echo "========================================="

# 1. 注册新用户
echo -e "\n[1] 注册新用户..."
echo "请求: POST ${BASE_URL}/user/register"
echo "参数: username=${USER_NAME}, email=${EMAIL}, password=${PASSWORD}, phone=${PHONE}"

REGISTER_RESPONSE=$(curl -s -X POST "${BASE_URL}/user/register" \
  -d "username=${USER_NAME}&email=${EMAIL}&password=${PASSWORD}&phone=${PHONE}")

echo "响应: ${REGISTER_RESPONSE}"

# 检查注册是否成功
if echo "${REGISTER_RESPONSE}" | grep -q '"ok":true' || echo "${REGISTER_RESPONSE}" | grep -q '"code":0'; then
    echo "✓ 注册成功"
else
    echo "✗ 注册失败"
    echo "错误信息: $(echo ${REGISTER_RESPONSE} | grep -o '"msg":"[^"]*"')"
    exit 1
fi

# 2. 等待一下确保数据已保存
sleep 2

# 3. 使用注册的用户名和密码登录
echo -e "\n[2] 用注册的用户名和密码登录..."
echo "请求: GET ${BASE_URL}/user/login"
echo "参数: username=${USER_NAME}, password=${PASSWORD}"

LOGIN_RESPONSE=$(curl -s -X GET "${BASE_URL}/user/login" \
  -d "username=${USER_NAME}&password=${PASSWORD}")

echo "响应: ${LOGIN_RESPONSE}"

# 检查登录是否成功
if echo "${LOGIN_RESPONSE}" | grep -q '"ok":true' || echo "${LOGIN_RESPONSE}" | grep -q '"code":0'; then
    echo "✓ 登录成功"
    echo "用户信息: $(echo ${LOGIN_RESPONSE} | grep -o '"data":{[^}]*}')"
else
    echo "✗ 登录失败"
    echo "错误信息: $(echo ${LOGIN_RESPONSE} | grep -o '"msg":"[^"]*"')"
    exit 1
fi

# 4. 尝试用相同邮箱再次注册（应该失败）
echo -e "\n[3] 尝试用相同邮箱再次注册（应该失败）..."
DUPLICATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/user/register" \
  -d "username=testuser456&email=${EMAIL}&password=anotherpass&phone=13800138001")

echo "响应: ${DUPLICATE_RESPONSE}"

if echo "${DUPLICATE_RESPONSE}" | grep -q '"ok":false' || echo "${DUPLICATE_RESPONSE}" | grep -q '"code":500'; then
    echo "✓ 正确拒绝了重复邮箱"
    echo "错误信息: $(echo ${DUPLICATE_RESPONSE} | grep -o '"msg":"[^"]*"')"
else
    echo "✗ 错误：应该拒绝重复邮箱，但却接受了"
    exit 1
fi

echo -e "\n========================================="
echo "所有测试通过！"
echo "========================================="
