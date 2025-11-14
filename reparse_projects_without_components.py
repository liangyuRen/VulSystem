#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
为没有组件的项目重新触发依赖解析
"""

import requests
import pymysql
import time
import sys

# Configuration
MYSQL_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '15256785749rly',
    'database': 'kulin',
    'charset': 'utf8mb4'
}

BACKEND_API = "http://localhost:8081/project/reparse"

def get_projects_without_components(conn):
    """获取没有组件的项目"""
    cursor = conn.cursor()
    cursor.execute("""
        SELECT p.id, p.name, p.language, COUNT(w.id) as component_count
        FROM project p
        LEFT JOIN white_list w ON p.file = w.file_path AND w.isdelete = 0
        WHERE p.isdelete = 0
        GROUP BY p.id, p.name, p.language
        HAVING component_count = 0
        ORDER BY p.language, p.id
    """)
    projects = cursor.fetchall()
    cursor.close()
    return projects

def trigger_reparse(project_id, language):
    """触发项目依赖重新解析"""
    try:
        data = {
            'projectId': project_id,
            'language': language
        }
        print(f"  -> Calling reparse API...")
        response = requests.post(BACKEND_API, data=data, timeout=10)

        if response.status_code == 200:
            result = response.json()
            if result.get('code') == 200:
                print(f"  [OK] Reparse triggered successfully")
                return True
            else:
                print(f"  [ERROR] API returned error: {result.get('message', 'Unknown error')}")
                return False
        else:
            print(f"  [ERROR] HTTP status: {response.status_code}")
            return False
    except Exception as e:
        print(f"  [ERROR] Failed: {e}")
        return False

def main():
    print("=" * 60)
    print("为没有组件的项目重新触发依赖解析")
    print("=" * 60)

    # Connect to database
    try:
        conn = pymysql.connect(**MYSQL_CONFIG)
        print("[OK] Database connected")
    except Exception as e:
        print(f"[ERROR] Database connection failed: {e}")
        sys.exit(1)

    # Get projects without components
    projects = get_projects_without_components(conn)
    conn.close()

    if not projects:
        print("\n[OK] All projects have components!")
        return

    print(f"\nFound {len(projects)} projects without components:\n")

    for project in projects:
        project_id, name, language, component_count = project
        print(f"Project ID {project_id}: {name} ({language})")

    print("\n" + "-" * 60)
    print("Starting reparse...\n")

    success_count = 0
    failed_count = 0
    skipped_count = 0

    for project in projects:
        project_id, name, language, component_count = project

        print("-" * 60)
        print(f"Project ID: {project_id}")
        print(f"Project Name: {name}")
        print(f"Language: {language}")

        # Skip 'other' and 'unknown' languages
        if language.lower() in ['other', 'unknown']:
            print(f"  [SKIP] Language '{language}' is not supported")
            skipped_count += 1
            continue

        # Trigger reparse
        if trigger_reparse(project_id, language):
            success_count += 1
            time.sleep(2)  # Wait between requests
        else:
            failed_count += 1

    # Summary
    print("\n" + "=" * 60)
    print("Reparse triggered!")
    print("=" * 60)
    print(f"Total projects without components: {len(projects)}")
    print(f"Triggered: {success_count}")
    print(f"Failed: {failed_count}")
    print(f"Skipped: {skipped_count}")
    print("=" * 60)
    print("\nWait 60 seconds for parsing to complete...")
    print("Then check: python -c \"import pymysql; conn=pymysql.connect(host='localhost',user='root',password='15256785749rly',database='kulin'); cursor=conn.cursor(); cursor.execute('SELECT language, COUNT(*) FROM white_list WHERE isdelete=0 GROUP BY language'); print(cursor.fetchall())\"")

if __name__ == "__main__":
    main()
