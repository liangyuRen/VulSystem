#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
诊断Flask语言检测和依赖解析功能
"""

import requests
import urllib.parse
import pymysql
import os
import json

MYSQL_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '15256785749rly',
    'database': 'kulin',
    'charset': 'utf8mb4'
}

FLASK_LANG_API = "http://localhost:5000/parse/get_primary_language"

def test_flask_language_detection(project_path, project_name):
    """测试Flask语言检测"""
    print(f"\n测试项目: {project_name}")
    print(f"路径: {project_path}")

    if not os.path.isdir(project_path):
        print(f"  [ERROR] 路径不存在!")
        return None

    # 检查配置文件
    print(f"  检查配置文件:")
    config_files = {
        'pom.xml': 'Java',
        'build.gradle': 'Java',
        'requirements.txt': 'Python',
        'setup.py': 'Python',
        'Cargo.toml': 'Rust',
        'go.mod': 'Go',
        'package.json': 'JavaScript',
        'composer.json': 'PHP',
        'Gemfile': 'Ruby',
        'rebar.config': 'Erlang',
        'Makefile': 'C/C++'
    }

    found_files = []
    for config_file, lang in config_files.items():
        file_path = os.path.join(project_path, config_file)
        if os.path.isfile(file_path):
            file_size = os.path.getsize(file_path)
            print(f"    [OK] 找到 {config_file} ({lang}) - {file_size} bytes")
            found_files.append((config_file, lang, file_size))

    if not found_files:
        print(f"    [WARNING] 没有找到任何配置文件")

    # 调用Flask API检测语言
    try:
        encoded_path = urllib.parse.quote(project_path)
        url = f"{FLASK_LANG_API}?project_folder={encoded_path}&use_optimized=true"

        print(f"  调用Flask API...")
        response = requests.get(url, timeout=10)

        if response.status_code == 200:
            data = response.json()
            detected_lang = data.get('language', 'unknown')
            print(f"  [OK] Flask检测到语言: {detected_lang}")
            return detected_lang
        else:
            print(f"  [ERROR] Flask API返回状态码: {response.status_code}")
            return None
    except Exception as e:
        print(f"  [ERROR] Flask API调用失败: {e}")
        return None

def get_projects_without_components():
    """获取没有组件的项目"""
    conn = pymysql.connect(**MYSQL_CONFIG)
    cursor = conn.cursor()
    cursor.execute("""
        SELECT p.id, p.name, p.language, p.file
        FROM project p
        LEFT JOIN white_list w ON p.file = w.file_path AND w.isdelete = 0
        WHERE p.isdelete = 0
        GROUP BY p.id, p.name, p.language, p.file
        HAVING COUNT(w.id) = 0
        ORDER BY p.language, p.id
    """)
    projects = cursor.fetchall()
    cursor.close()
    conn.close()
    return projects

def main():
    print("=" * 70)
    print("Flask语言检测和依赖解析诊断")
    print("=" * 70)

    projects = get_projects_without_components()

    if not projects:
        print("\n[OK] 所有项目都有组件!")
        return

    print(f"\n找到 {len(projects)} 个没有组件的项目\n")

    results = []

    for project in projects:
        project_id, name, db_language, file_path = project

        print("-" * 70)
        print(f"项目ID: {project_id}")
        print(f"项目名称: {name}")
        print(f"数据库中的语言: {db_language}")

        detected_lang = test_flask_language_detection(file_path, name)

        results.append({
            'id': project_id,
            'name': name,
            'db_language': db_language,
            'detected_language': detected_lang,
            'match': db_language == detected_lang if detected_lang else False
        })

    # 总结
    print("\n" + "=" * 70)
    print("诊断总结")
    print("=" * 70)

    print(f"\n{'ID':<5} {'数据库语言':<15} {'检测语言':<15} {'匹配':<10}")
    print("-" * 70)

    match_count = 0
    mismatch_count = 0
    failed_count = 0

    for result in results:
        match_str = ""
        if result['detected_language']:
            if result['match']:
                match_str = "[OK]"
                match_count += 1
            else:
                match_str = "[MISMATCH]"
                mismatch_count += 1
        else:
            match_str = "[FAILED]"
            failed_count += 1

        detected = result['detected_language'] or "N/A"
        print(f"{result['id']:<5} {result['db_language']:<15} {detected:<15} {match_str:<10}")

    print("\n" + "-" * 70)
    print(f"匹配: {match_count}")
    print(f"不匹配: {mismatch_count}")
    print(f"检测失败: {failed_count}")
    print("=" * 70)

    # 建议
    print("\n建议:")
    print("1. 对于没有配置文件的项目，可能是测试项目或不完整的项目")
    print("2. Rust项目可能因为依赖太多导致Flask超时")
    print("3. JavaScript项目可能缺少package.json")
    print("4. Erlang项目的rebar.config可能没有定义依赖")

if __name__ == "__main__":
    main()
