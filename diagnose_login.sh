#!/bin/bash

# 登录接口诊断脚本
# 用于诊断为什么前端登录失败

BASE_URL="http://localhost:8081"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                  登录接口诊断工具                              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# 检查参数
if [ $# -ne 2 ]; then
    echo "用法: $0 <username> <password>"
    echo ""
    echo "示例:"
    echo "  $0 admin admin"
    echo ""
    exit 1
fi

USERNAME="$1"
PASSWORD="$2"

echo "📝 测试信息"
echo "───────────────────────────────────────────────────────────────"
echo "后端地址: $BASE_URL"
echo "用户名: $USERNAME"
echo "密码: $(printf '*%.0s' {1..${#PASSWORD}})"
echo ""

# 第 1 步: 检查后端服务是否可用
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ 步骤 1: 检查后端服务连接"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 测试连接
if ! timeout 5 bash -c "echo > /dev/tcp/localhost/8081" 2>/dev/null; then
    echo "❌ 后端服务不可用"
    echo ""
    echo "可能的原因:"
    echo "  1. 后端没有启动"
    echo "  2. 后端运行在不同的端口"
    echo "  3. 防火墙阻止了连接"
    echo ""
    echo "解决方案:"
    echo "  1. 启动后端: java -jar backend/target/backend-*.jar"
    echo "  2. 或使用 Maven: cd backend && mvn spring-boot:run -DskipTests"
    exit 1
fi

echo "✅ 后端服务运行正常"
echo ""

# 第 2 步: 测试 GET 方式的登录接口
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ 步骤 2: 测试 GET 方式登录"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "发送请求:"
echo "  GET ${BASE_URL}/user/login"
echo "  参数: username=$USERNAME&password=$PASSWORD"
echo ""

GET_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/user/login" \
    -d "username=${USERNAME}&password=${PASSWORD}")

# 分离 HTTP 状态码和响应体
HTTP_CODE=$(echo "$GET_RESPONSE" | tail -1)
RESPONSE_BODY=$(echo "$GET_RESPONSE" | head -n -1)

echo "HTTP 状态码: $HTTP_CODE"
echo ""
echo "响应内容:"
echo "$RESPONSE_BODY" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE_BODY"
echo ""

# 分析响应
if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ HTTP 状态正常 (200 OK)"

    if echo "$RESPONSE_BODY" | grep -q '"code":200'; then
        echo "✅ 登录成功 (code: 200) - 后端工作正常"
    elif echo "$RESPONSE_BODY" | grep -q '"code":0'; then
        echo "✅ 登录成功 (code: 0)"
    elif echo "$RESPONSE_BODY" | grep -q '"ok":true'; then
        echo "✅ 登录成功 (ok: true)"
    else
        echo "❌ 登录失败"

        if echo "$RESPONSE_BODY" | grep -q "用户名或密码错误"; then
            echo "原因: 用户名或密码错误"
        elif echo "$RESPONSE_BODY" | grep -q "用户名不存在"; then
            echo "原因: 用户名不存在"
        else
            echo "原因: 未知错误"
        fi
    fi
else
    echo "❌ HTTP 状态异常 ($HTTP_CODE)"
    echo "可能的原因:"
    echo "  - 后端出错"
    echo "  - 路由不存在"
    echo "  - 请求格式不正确"
fi

echo ""

# 第 3 步: 测试 POST 方式的登录接口（某些前端可能使用 POST）
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ 步骤 3: 测试 POST 方式登录"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "发送请求:"
echo "  POST ${BASE_URL}/user/login"
echo "  Content-Type: application/x-www-form-urlencoded"
echo ""

POST_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/user/login" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=${USERNAME}&password=${PASSWORD}")

# 分离 HTTP 状态码和响应体
POST_HTTP_CODE=$(echo "$POST_RESPONSE" | tail -1)
POST_RESPONSE_BODY=$(echo "$POST_RESPONSE" | head -n -1)

echo "HTTP 状态码: $POST_HTTP_CODE"
echo ""
echo "响应内容:"
echo "$POST_RESPONSE_BODY" | python3 -m json.tool 2>/dev/null || echo "$POST_RESPONSE_BODY"
echo ""

if [ "$POST_HTTP_CODE" != "200" ] && [ "$POST_HTTP_CODE" != "404" ]; then
    if echo "$POST_RESPONSE_BODY" | grep -q '"code":0'; then
        echo "✅ POST 方式登录可用"
    elif echo "$POST_RESPONSE_BODY" | grep -q '"ok":true'; then
        echo "✅ POST 方式登录可用"
    fi
fi

echo ""

# 第 4 步: 查询用户是否存在
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ 步骤 4: 验证用户在数据库中是否存在"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 尝试查询用户信息
USER_INFO=$(mysql -u root -p 2>/dev/null <<EOF
SELECT user_name, email, role, isvalid, isdelete FROM user WHERE user_name = '$USERNAME' AND isdelete = 0;
EOF
)

if [ -z "$USER_INFO" ]; then
    echo "❌ 用户 '$USERNAME' 不存在或已被删除"
    echo ""
    echo "可能的原因:"
    echo "  1. 用户名拼写错误"
    echo "  2. 用户已被删除"
    echo "  3. 用户尚未注册"
    echo ""
    echo "解决方案:"
    echo "  1. 查看所有用户: mysql -u root -p kulin < query_users.sql"
    echo "  2. 或使用命令: mysql -u root -p kulin -e \"SELECT user_name FROM user WHERE isdelete = 0;\""
else
    echo "✅ 用户存在"
    echo ""
    echo "用户信息:"
    echo "$USER_INFO"
fi

echo ""

# 第 5 步: 检查密码加密
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ 步骤 5: 验证密码加密存储"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

PASSWORD_HASH=$(mysql -u root -p 2>/dev/null <<EOF
SELECT password FROM user WHERE user_name = '$USERNAME' AND isdelete = 0 LIMIT 1;
EOF
)

if [ -z "$PASSWORD_HASH" ]; then
    echo "❌ 无法获取用户密码哈希"
else
    echo "✅ 密码以 BCrypt 哈希形式存储"
    echo ""
    echo "密码哈希摘要: ${PASSWORD_HASH:0:30}..."
    echo ""
    echo "💡 说明:"
    echo "  - 密码使用 BCrypt 加密算法存储"
    echo "  - 后端会在验证时自动比对密码"
    echo "  - 您的输入密码 '$PASSWORD' 会被加密后与哈希值比对"
fi

echo ""

# 第 6 步: 测试其他用户
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ 步骤 6: 尝试其他已知用户"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "常见的测试账户:"
echo ""

# 测试 admin/admin
echo "测试: admin/admin"
TEST_RESPONSE=$(curl -s -X GET "$BASE_URL/user/login" \
    -d "username=admin&password=admin")

if echo "$TEST_RESPONSE" | grep -q '"code":0'; then
    echo "  ✅ admin 账户可用"
elif echo "$TEST_RESPONSE" | grep -q '"ok":true'; then
    echo "  ✅ admin 账户可用"
else
    echo "  ❌ admin 账户不可用或密码错误"
fi

echo ""

# 第 7 步: 检查前端请求格式
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✓ 步骤 7: 前端请求检查清单"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "请确认前端代码中:"
echo ""
echo "□ API 端点是否正确"
echo "  正确: http://localhost:8081/user/login"
echo "  ❌ 错误端口: http://localhost:8080/user/login (那是 XXL-Job 的端口)"
echo ""
echo "□ 请求方法是否正确"
echo "  GET: http://localhost:8081/user/login?username=...&password=..."
echo "  或 POST 表单数据"
echo ""
echo "□ 参数名是否匹配"
echo "  ✅ 正确: username 和 password"
echo "  ❌ 错误: user, name, pwd, pass 等"
echo ""
echo "□ 响应处理是否正确"
echo "  应该检查 code === 0 或 ok === true"
echo "  ❌ 不能只检查 HTTP 200"
echo ""
echo "□ CORS 跨域是否配置"
echo "  后端已配置 CORS，应该可以跨域请求"
echo ""

echo ""

# 总结
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                        诊断总结                                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# 确定最可能的原因
if echo "$RESPONSE_BODY" | grep -q '"code":0'; then
    echo "✅ 后端登录接口工作正常"
    echo ""
    echo "如果前端仍然显示失败，问题可能在:"
    echo "  1. 前端没有正确处理响应"
    echo "  2. 前端使用了错误的端口 (8080 而不是 8081)"
    echo "  3. CORS 跨域问题"
    echo "  4. 前端代码逻辑错误"
    echo ""
    echo "解决方案:"
    echo "  - 打开浏览器开发者工具 (F12)"
    echo "  - 查看 Network 标签中的请求"
    echo "  - 检查请求 URL、方法、参数是否正确"
    echo "  - 检查响应状态码和内容"
elif echo "$RESPONSE_BODY" | grep -q "用户名或密码错误"; then
    echo "❌ 密码错误"
    echo ""
    echo "解决方案:"
    echo "  1. 确认密码是否正确"
    echo "  2. 如果忘记密码，需要重置"
    echo "  3. 尝试其他已知账户测试"
elif echo "$RESPONSE_BODY" | grep -q "用户名不存在"; then
    echo "❌ 用户不存在"
    echo ""
    echo "解决方案:"
    echo "  1. 确认用户名拼写正确"
    echo "  2. 查看数据库中的用户列表"
    echo "  3. 如果用户不存在，需要先注册"
else
    echo "⚠️ 无法确定具体原因"
    echo ""
    echo "下一步:"
    echo "  1. 查看后端日志 (logs/ 目录或 stdout)"
    echo "  2. 检查数据库连接"
    echo "  3. 检查前端网络请求 (F12 开发者工具)"
fi

echo ""
echo "═══════════════════════════════════════════════════════════════════"
