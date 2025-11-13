#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试多语言项目解析
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

# 要测试的项目
TEST_PROJECTS = [
    {'id': 32, 'language': 'python', 'name': 'Python测试项目'},
    {'id': 29, 'language': 'go', 'name': 'Go测试项目'},
    {'id': 31, 'language': 'rust', 'name': 'Rust测试项目'},
    {'id': 27, 'language': 'javascript', 'name': 'JavaScript测试项目'},
    {'id': 30, 'language': 'php', 'name': 'PHP测试项目'},
    {'id': 33, 'language': 'ruby', 'name': 'Ruby测试项目'},
    {'id': 28, 'language': 'erlang', 'name': 'Erlang测试项目'},
]

def print_section(title):
    print("\n" + "=" * 80)
    print(f"  {title}")
    print("=" * 80)

def count_dependencies(conn, language):
    """统计指定语言的依赖数量"""
    cursor = conn.cursor()
    cursor.execute(
        "SELECT COUNT(*) FROM white_list WHERE language = %s AND isdelete = 0",
        (language.lower(),)
    )
    count = cursor.fetchone()[0]
    cursor.close()
    return count

def trigger_reparse(project_id, language):
    """触发项目重新解析"""
    url = f"{BACKEND_URL}/project/reparse"
    data = {
        'projectId': project_id,
        'language': language
    }

    try:
        resp = requests.post(url, data=data, timeout=30)
        if resp.status_code == 200:
            result = resp.json()
            if result.get('code') == 200:
                return True, result
            else:
                return False, result
        else:
            return False, {'error': f'HTTP {resp.status_code}'}
    except Exception as e:
        return False, {'error': str(e)}

def show_dependencies(conn, language, limit=5):
    """显示最新的依赖记录"""
    cursor = conn.cursor(dictionary=True)
    cursor.execute("""
        SELECT id, name, language, description
        FROM white_list
        WHERE language = %s AND isdelete = 0
        ORDER BY id DESC
        LIMIT %s
    """, (language.lower(), limit))

    entries = cursor.fetchall()
    cursor.close()

    if entries:
        print(f"\n最新的{language}依赖 (最多{limit}条):")
        for entry in entries:
            print(f"  [{entry['id']}] {entry['name']}")
            if entry['description']:
                desc = entry['description'][:80] + "..." if len(entry['description']) > 80 else entry['description']
                print(f"      {desc}")
    else:
        print(f"\n未找到{language}依赖记录")

def test_project(conn, project):
    """测试单个项目"""
    print_section(f"测试 {project['name']} (ID: {project['id']})")

    language = project['language'].lower()

    # 解析前统计
    before_count = count_dependencies(conn, language)
    print(f"解析前 {language} 依赖数: {before_count}")

    # 触发解析
    print(f"\n正在触发 {language} 项目解析...")
    success, result = trigger_reparse(project['id'], language)

    if success:
        print(f"[成功] 解析请求已提交")
        print(f"响应: {result.get('obj', {}).get('message', 'N/A')}")

        # 等待解析完成
        print("\n等待异步解析完成 (15秒)...", end='')
        for i in range(15):
            time.sleep(1)
            print(".", end='', flush=True)
        print(" 完成")

        # 解析后统计
        after_count = count_dependencies(conn, language)
        new_count = after_count - before_count

        print(f"\n解析后 {language} 依赖数: {after_count}")

        if new_count > 0:
            print(f"[✓✓✓ 成功] 新增了 {new_count} 条 {language} 依赖到white_list表！")
            show_dependencies(conn, language, 5)
            return True
        elif after_count > 0:
            print(f"[提示] 数据库中已有 {after_count} 条记录，但本次没有新增")
            print("       (可能该项目之前已解析过)")
            show_dependencies(conn, language, 3)
            return True
        else:
            print(f"[失败] 没有找到任何 {language} 依赖记录")
            print("请检查Spring Boot控制台日志查看详细错误信息")
            return False
    else:
        print(f"[失败] 解析请求失败: {result}")
        return False

def main():
    print("=" * 80)
    print("  多语言项目依赖解析测试")
    print("=" * 80)

    # 连接数据库
    print("\n连接数据库...")
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        print("[成功] 数据库连接成功")
    except Exception as e:
        print(f"[失败] 数据库连接失败: {e}")
        return

    # 显示当前统计
    print_section("当前数据库统计")
    cursor = conn.cursor()
    cursor.execute("""
        SELECT language, COUNT(*) as cnt
        FROM white_list
        WHERE isdelete = 0
        GROUP BY language
        ORDER BY cnt DESC
    """)
    results = cursor.fetchall()

    print("\n各语言依赖统计:")
    total = 0
    for lang, cnt in results:
        print(f"  {lang:15s}: {cnt:5d} 条")
        total += cnt
    print(f"  {'总计':15s}: {total:5d} 条")
    cursor.close()

    # 测试每个项目
    test_results = []
    for project in TEST_PROJECTS:
        result = test_project(conn, project)
        test_results.append((project['language'], result))
        time.sleep(2)  # 间隔2秒再测试下一个

    # 最终统计
    print_section("测试结果汇总")

    success_count = sum(1 for _, result in test_results if result)
    fail_count = len(test_results) - success_count

    for language, result in test_results:
        status = "[✓] 成功" if result else "[✗] 失败"
        print(f"{status:12s} {language}")

    print(f"\n总计: {len(test_results)} 个项目")
    print(f"成功: {success_count}")
    print(f"失败: {fail_count}")

    # 最终数据库统计
    print_section("最终数据库统计")
    cursor = conn.cursor()
    cursor.execute("""
        SELECT language, COUNT(*) as cnt
        FROM white_list
        WHERE isdelete = 0
        GROUP BY language
        ORDER BY cnt DESC
    """)
    results = cursor.fetchall()

    print("\n各语言依赖统计:")
    total = 0
    for lang, cnt in results:
        print(f"  {lang:15s}: {cnt:5d} 条")
        total += cnt
    print(f"  {'总计':15s}: {total:5d} 条")
    cursor.close()

    conn.close()

    print("\n" + "=" * 80)
    print("测试完成！")
    print("=" * 80)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n测试被用户中断")
    except Exception as e:
        print(f"\n测试过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
