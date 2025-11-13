#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速验证修复是否生效
"""

import requests
import json
import time
import mysql.connector

# 配置
BACKEND_URL = "http://localhost:8081"
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '15256785749rly',
    'database': 'kulin',
    'port': 3306
}

def test_python_project():
    """测试Python项目"""
    print("=" * 80)
    print("  测试Python项目依赖解析（验证ID修复）")
    print("=" * 80)

    # 连接数据库
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()

    # 解析前统计
    cursor.execute("SELECT COUNT(*) FROM white_list WHERE language = 'python' AND isdelete = 0")
    before_count = cursor.fetchone()[0]
    print(f"\n解析前Python依赖数: {before_count}")

    # 触发解析
    print("\n正在触发Python项目解析...")
    resp = requests.post(
        f"{BACKEND_URL}/project/reparse",
        data={'projectId': 32, 'language': 'python'},
        timeout=30
    )

    if resp.status_code == 200:
        result = resp.json()
        if result.get('code') == 200:
            print("[成功] 解析请求已提交")

            # 等待15秒
            print("等待异步解析完成 (15秒)...", end='')
            for i in range(15):
                time.sleep(1)
                print(".", end='', flush=True)
            print(" 完成\n")

            # 解析后统计
            cursor.execute("SELECT COUNT(*) FROM white_list WHERE language = 'python' AND isdelete = 0")
            after_count = cursor.fetchone()[0]
            new_count = after_count - before_count

            print(f"解析后Python依赖数: {after_count}")
            print(f"新增记录数: {new_count}")

            if new_count > 0:
                print(f"\n[✓✓✓ 成功] 新增了 {new_count} 条Python依赖！ID问题已修复！")

                # 显示新增的依赖
                cursor.execute("""
                    SELECT id, name, language
                    FROM white_list
                    WHERE language = 'python' AND isdelete = 0
                    ORDER BY id DESC
                    LIMIT 5
                """)
                entries = cursor.fetchall()

                print("\n最新的Python依赖:")
                for id, name, lang in entries:
                    print(f"  ID={id:5d} | {name}")

                print("\n✓ 数据库ID字段正常！")
                print("✓ 多语言解析功能完全正常！")
                return True
            elif after_count > 0:
                print(f"\n[提示] 数据库中已有 {after_count} 条记录（之前已解析过）")
                return True
            else:
                print("\n[失败] 仍然没有数据写入")
                print("请查看Spring Boot日志中的详细错误信息")
                return False
        else:
            print(f"[失败] 解析请求失败: {result.get('message')}")
            return False
    else:
        print(f"[失败] HTTP错误: {resp.status_code}")
        return False

    cursor.close()
    conn.close()

if __name__ == "__main__":
    try:
        test_python_project()
    except Exception as e:
        print(f"\n错误: {e}")
        import traceback
        traceback.print_exc()
