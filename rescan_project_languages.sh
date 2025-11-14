#!/bin/bash

# 重新扫描所有项目的语言类型并更新数据库
# 使用Flask API进行语言检测

MYSQL_USER="root"
MYSQL_PASS="15256785749rly"
MYSQL_DB="kulin"
FLASK_API="http://localhost:5000/parse/get_primary_language"

echo "=========================================="
echo "开始重新扫描所有项目的语言类型"
echo "=========================================="

# 获取所有未删除的项目
mysql -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -N -e "SELECT id, name, file FROM project WHERE isdelete = 0 ORDER BY id;" | while read -r id name file_path; do
    echo ""
    echo "----------------------------------------"
    echo "项目 ID: $id"
    echo "项目名称: $name"
    echo "项目路径: $file_path"

    # 检查路径是否存在
    if [ ! -d "$file_path" ]; then
        echo "⚠ 警告: 路径不存在，跳过"
        continue
    fi

    # URL编码路径
    encoded_path=$(python -c "import urllib.parse; print(urllib.parse.quote('''$file_path'''))")

    # 调用Flask API检测语言
    echo "→ 调用Flask API检测语言..."
    response=$(timeout 10 curl -s "${FLASK_API}?project_folder=${encoded_path}&use_optimized=true")

    if [ $? -eq 0 ] && [ -n "$response" ]; then
        # 解析JSON响应
        detected_lang=$(echo "$response" | python -c "import sys, json; data=json.load(sys.stdin); print(data.get('language', 'unknown'))")

        if [ "$detected_lang" != "unknown" ] && [ -n "$detected_lang" ]; then
            echo "✓ 检测到语言: $detected_lang"

            # 获取当前数据库中的语言
            current_lang=$(mysql -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -N -e "SELECT language FROM project WHERE id = $id;")

            if [ "$detected_lang" != "$current_lang" ]; then
                echo "⚠ 语言不匹配！数据库: $current_lang → 实际: $detected_lang"
                echo "→ 更新数据库..."

                mysql -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -e "UPDATE project SET language = '$detected_lang' WHERE id = $id;"

                if [ $? -eq 0 ]; then
                    echo "✓ 更新成功: $current_lang → $detected_lang"
                else
                    echo "✗ 更新失败"
                fi
            else
                echo "✓ 语言正确，无需更新"
            fi
        else
            echo "⚠ Flask API返回unknown，跳过更新"
        fi
    else
        echo "✗ Flask API调用失败或超时"
    fi
done

echo ""
echo "=========================================="
echo "重新扫描完成！"
echo "=========================================="
echo ""
echo "更新后的项目列表:"
mysql -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -e "SELECT id, name, language FROM project WHERE isdelete = 0 ORDER BY id;"
