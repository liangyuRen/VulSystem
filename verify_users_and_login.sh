#!/bin/bash

# 数据库用户验证和登录测试脚本
# 这个脚本将:
# 1. 连接到 MySQL 数据库查询用户信息
# 2. 用每个用户进行登录测试
# 3. 验证登录是否成功

BASE_URL="http://localhost:8081"
MYSQL_USER="root"
MYSQL_PASSWORD="root"
MYSQL_HOST="localhost"
DB_NAME="kulin"

echo "════════════════════════════════════════════════════════════════"
echo "               数据库用户验证和登录测试"
echo "════════════════════════════════════════════════════════════════"
echo ""

# 1. 查询数据库中的所有用户
echo "📋 步骤 1: 查询数据库中的用户信息"
echo "─────────────────────────────────────────────────────────────"

# 构建 MySQL 查询命令
QUERY="SELECT id, user_name, email, password, role, isdelete, isvalid FROM user WHERE isdelete = 0 LIMIT 10;"

# 尝试连接到数据库
echo "连接到数据库: $MYSQL_HOST / $DB_NAME"
USERS=$(mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$DB_NAME" -e "$QUERY" 2>&1)

if echo "$USERS" | grep -q "ERROR"; then
    echo "❌ 数据库连接失败"
    echo "错误信息: $USERS"
    echo ""
    echo "请确保:"
    echo "  - MySQL 服务正在运行"
    echo "  - 数据库凭证正确 (用户: $MYSQL_USER)"
    echo "  - 数据库存在 (数据库: $DB_NAME)"
    exit 1
fi

echo "✓ 数据库连接成功"
echo ""
echo "用户列表:"
echo "$USERS"
echo ""

# 2. 提取用户名和密码，进行登录测试
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📝 步骤 2: 逐个用户进行登录测试"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# 获取用户名列表（跳过表头）
USERNAMES=$(mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$DB_NAME" \
    -sN -e "SELECT user_name FROM user WHERE isdelete = 0 LIMIT 10;" 2>&1)

if [ -z "$USERNAMES" ]; then
    echo "❌ 没有找到任何用户"
    exit 1
fi

# 计数器
SUCCESS_COUNT=0
FAIL_COUNT=0
TEST_NUM=0

# 对每个用户进行登录测试
while IFS= read -r USERNAME; do
    if [ -z "$USERNAME" ]; then
        continue
    fi

    TEST_NUM=$((TEST_NUM + 1))

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "测试 #$TEST_NUM: 用户名 = $USERNAME"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    # 注意: 我们不知道密码，所以需要尝试常见的密码或从数据库中获取
    # 首先尝试用用户名作为密码（常见做法）
    PASSWORD="$USERNAME"

    echo "尝试登录"
    echo "  API: POST $BASE_URL/user/login"
    echo "  用户名: $USERNAME"
    echo "  密码: $PASSWORD (尝试用户名作为密码)"
    echo ""

    # 调用登录 API
    LOGIN_RESPONSE=$(curl -s -X GET "$BASE_URL/user/login" \
        -d "username=${USERNAME}&password=${PASSWORD}")

    echo "API 响应:"

    # 尝试格式化 JSON 输出
    if command -v python3 &> /dev/null; then
        echo "$LOGIN_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$LOGIN_RESPONSE"
    else
        echo "$LOGIN_RESPONSE"
    fi

    echo ""

    # 检查是否登录成功
    if echo "$LOGIN_RESPONSE" | grep -q '"code":0'; then
        echo "✅ 登录成功！"
        echo "   用户信息已返回"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    elif echo "$LOGIN_RESPONSE" | grep -q '"ok":true'; then
        echo "✅ 登录成功！"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo "❌ 登录失败"
        echo "   可能原因:"

        if echo "$LOGIN_RESPONSE" | grep -q "用户名或密码错误"; then
            echo "   - 密码错误（$PASSWORD 不是正确的密码）"
        elif echo "$LOGIN_RESPONSE" | grep -q "用户名不存在"; then
            echo "   - 用户名不存在"
        else
            echo "   - 未知错误"
        fi

        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi

    echo ""

done <<< "$USERNAMES"

echo "════════════════════════════════════════════════════════════════"
echo "                         测试总结"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "总测试数: $TEST_NUM"
echo "成功: $SUCCESS_COUNT ✅"
echo "失败: $FAIL_COUNT ❌"
echo ""

if [ $FAIL_COUNT -gt 0 ]; then
    echo "⚠️  注意: 有些用户登录失败，可能的原因:"
    echo "  1. 密码不是用户名"
    echo "  2. 后端服务未运行或端口不对"
    echo "  3. 密码被加密存储，无法直接查看"
    echo ""
    echo "💡 建议:"
    echo "  1. 检查后端服务是否运行在 http://localhost:8081"
    echo "  2. 查看密码的实际值（MySQL 中的密码应该是 BCrypt 哈希）"
    echo "  3. 使用已知密码的测试账户进行登录"
fi

echo ""
echo "════════════════════════════════════════════════════════════════"
