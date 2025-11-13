#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Python项目解析诊断脚本
"""

import requests
import json
import time
import mysql.connector
from urllib.parse import quote

# 配置
BACKEND_URL = "http://localhost:8081"
FLASK_URL = "http://localhost:5000"
PROJECT_ID = 32  # Python测试项目
PROJECT_PATH = r"D:\kuling\upload\66dd438b-44bb-4cf0-98ab-5f302c461099"

DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '15256785749rly',
    'database': 'kulin',
    'port': 3306
}

def print_section(title):
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)

def check_services():
    """检查服务状态"""
    print_section("步骤1: 检查服务状态")

    # 检查Flask
    try:
        resp = requests.get(f"{FLASK_URL}/vulnerabilities/test", timeout=5)
        if resp.status_code == 200:
            print("[OK] Flask服务运行正常")
        else:
            print(f"[FAIL] Flask服务异常: {resp.status_code}")
            return False
    except Exception as e:
        print(f"[FAIL] Flask服务连接失败: {e}")
        return False

    # 检查Spring Boot
    try:
        resp = requests.get(f"{BACKEND_URL}/project/statistics?companyId=1", timeout=5)
        if resp.status_code == 200:
            print("[OK] Spring Boot服务运行正常")
        else:
            print(f"[FAIL] Spring Boot服务异常: {resp.status_code}")
            return False
    except Exception as e:
        print(f"[FAIL] Spring Boot服务连接失败: {e}")
        return False

    return True

def test_flask_api():
    """测试Flask API"""
    print_section("步骤2: 测试Flask API")

    url = f"{FLASK_URL}/parse/python_parse"
    params = {'project_folder': PROJECT_PATH}

    print(f"URL: {url}")
    print(f"参数: {params}")
    print()

    try:
        resp = requests.get(url, params=params, timeout=30)

        if resp.status_code == 200:
            print("[OK] Flask API调用成功")
            print(f"响应长度: {len(resp.text)} 字符")
            print()

            data = resp.json()
            print(f"[OK] JSON解析成功")
            print(f"依赖数量: {len(data)}")
            print()

            if len(data) > 0:
                print("前3个依赖:")
                for i, dep in enumerate(data[:3], 1):
                    print(f"{i}. {dep.get('name', 'N/A')}")
                    print(f"   描述: {dep.get('description', 'N/A')[:80]}...")
                print()

                # 显示完整的第一个依赖的JSON结构
                print("第一个依赖的完整JSON:")
                print(json.dumps(data[0], indent=2, ensure_ascii=False))
                return True, data
            else:
                print("[WARN] Flask返回空数组")
                return False, []
        else:
            print(f"[FAIL] Flask API返回错误: {resp.status_code}")
            print(f"响应: {resp.text[:500]}")
            return False, []

    except Exception as e:
        print(f"[FAIL] Flask API测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False, []

def check_database_before():
    """检查数据库当前状态"""
    print_section("步骤3: 检查数据库当前状态")

    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # 总数
        cursor.execute("SELECT COUNT(*) FROM white_list WHERE isdelete = 0")
        total = cursor.fetchone()[0]
        print(f"white_list总记录数: {total}")

        # Python记录数
        cursor.execute("SELECT COUNT(*) FROM white_list WHERE language = 'python' AND isdelete = 0")
        python_count = cursor.fetchone()[0]
        print(f"Python依赖记录数: {python_count}")

        # 按语言统计
        cursor.execute("""
            SELECT language, COUNT(*) as cnt
            FROM white_list
            WHERE isdelete = 0
            GROUP BY language
            ORDER BY cnt DESC
        """)
        results = cursor.fetchall()
        print("\n各语言统计:")
        for lang, cnt in results:
            print(f"  {lang}: {cnt}")

        cursor.close()
        conn.close()

        return python_count

    except Exception as e:
        print(f"[FAIL] 数据库查询失败: {e}")
        return 0

def trigger_reparse():
    """触发重新解析"""
    print_section("步骤4: 触发Python项目重新解析")

    url = f"{BACKEND_URL}/project/reparse"
    data = {
        'projectId': PROJECT_ID,
        'language': 'python'
    }

    print(f"URL: {url}")
    print(f"参数: {data}")
    print()

    try:
        resp = requests.post(url, data=data, timeout=30)

        if resp.status_code == 200:
            result = resp.json()
            print("[OK] 重新解析请求成功")
            print(f"响应: {json.dumps(result, indent=2, ensure_ascii=False)}")

            if result.get('code') == 200:
                return True
            else:
                print(f"[FAIL] 服务端返回错误: {result.get('message')}")
                return False
        else:
            print(f"[FAIL] HTTP错误: {resp.status_code}")
            print(f"响应: {resp.text[:500]}")
            return False

    except Exception as e:
        print(f"[FAIL] 触发解析失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def check_database_after():
    """检查解析后的数据库状态"""
    print_section("步骤5: 检查解析后的数据库")

    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        cursor = conn.cursor(dictionary=True)

        # Python记录数
        cursor.execute("SELECT COUNT(*) FROM white_list WHERE language = 'python' AND isdelete = 0")
        python_count = cursor.fetchone()['COUNT(*)']
        print(f"Python依赖记录数: {python_count}")

        if python_count > 0:
            # 显示Python依赖
            cursor.execute("""
                SELECT id, name, language, file_path, description
                FROM white_list
                WHERE language = 'python' AND isdelete = 0
                ORDER BY id DESC
                LIMIT 10
            """)
            entries = cursor.fetchall()

            print(f"\nPython依赖列表 (最多显示10条):")
            for entry in entries:
                print(f"  ID={entry['id']}, Name={entry['name']}")
                print(f"    Path={entry['file_path']}")
                if entry['description']:
                    print(f"    Desc={entry['description'][:60]}...")
                print()

        cursor.close()
        conn.close()

        return python_count

    except Exception as e:
        print(f"[FAIL] 数据库查询失败: {e}")
        return 0

def main():
    print("=" * 70)
    print("  Python项目依赖解析诊断")
    print("=" * 70)
    print(f"项目ID: {PROJECT_ID}")
    print(f"项目路径: {PROJECT_PATH}")
    print()

    # 步骤1: 检查服务
    if not check_services():
        print("\n[错误] 服务检查失败，请确保Flask和Spring Boot都在运行")
        return

    # 步骤2: 测试Flask API
    flask_ok, flask_data = test_flask_api()
    if not flask_ok or len(flask_data) == 0:
        print("\n[错误] Flask API未返回依赖数据")
        return

    # 步骤3: 检查数据库（解析前）
    before_count = check_database_before()

    # 步骤4: 触发解析
    if not trigger_reparse():
        print("\n[错误] 触发解析失败")
        return

    # 等待异步任务完成
    print("\n等待异步任务完成（20秒）...")
    for i in range(20):
        time.sleep(1)
        print(f"  {i+1}/20...", end='\r')
    print()

    # 步骤5: 检查数据库（解析后）
    after_count = check_database_after()

    # 结果
    print_section("诊断结果")

    print(f"Flask API返回依赖数: {len(flask_data)}")
    print(f"解析前Python记录数: {before_count}")
    print(f"解析后Python记录数: {after_count}")
    print(f"新增记录数: {after_count - before_count}")
    print()

    if after_count > before_count:
        print("[成功] 数据成功写入white_list表！")
        print(f"  新增了 {after_count - before_count} 条Python依赖记录")
    elif after_count > 0:
        print("[警告] 数据库中已有Python记录，但本次没有新增")
        print("  可能原因: 该项目之前已解析过")
    else:
        print("[失败] 数据未写入white_list表！")
        print("\n可能的原因:")
        print("  1. Spring Boot异步任务未执行")
        print("  2. JSON解析失败")
        print("  3. 数据库插入失败")
        print("  4. Spring Boot代码未更新（需要重启服务）")
        print("\n建议:")
        print("  1. 重启Spring Boot服务")
        print("  2. 查看Spring Boot控制台日志")
        print("  3. 检查是否有异常堆栈输出")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n诊断被用户中断")
    except Exception as e:
        print(f"\n诊断过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
