#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Trigger dependency re-parsing for all projects
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

def get_all_projects(conn):
    """Get all non-deleted projects"""
    cursor = conn.cursor()
    cursor.execute("SELECT id, name, language, file FROM project WHERE isdelete = 0 ORDER BY id")
    projects = cursor.fetchall()
    cursor.close()
    return projects

def trigger_reparse(project_id, language):
    """Trigger project dependency re-parsing via backend API"""
    try:
        data = {
            'projectId': project_id,
            'language': language
        }
        print(f"  -> Calling reparse API for {language}...")
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
    except requests.exceptions.Timeout:
        print(f"  [ERROR] API timeout")
        return False
    except Exception as e:
        print(f"  [ERROR] API call failed: {e}")
        return False

def main():
    print("=" * 60)
    print("Trigger dependency re-parsing for all projects")
    print("=" * 60)

    # Connect to database
    try:
        conn = pymysql.connect(**MYSQL_CONFIG)
        print("[OK] Database connected")
    except Exception as e:
        print(f"[ERROR] Database connection failed: {e}")
        sys.exit(1)

    # Get all projects
    projects = get_all_projects(conn)
    print(f"\nFound {len(projects)} projects to reparse\n")

    conn.close()

    success_count = 0
    failed_count = 0
    skipped_count = 0

    for project in projects:
        project_id, name, language, file_path = project

        print("-" * 60)
        print(f"Project ID: {project_id}")
        print(f"Project Name: {name}")
        print(f"Language: {language}")

        # Skip 'other' and 'unknown' languages
        if language.lower() in ['other', 'unknown']:
            print(f"  [SKIP] Language '{language}' is not supported for parsing")
            skipped_count += 1
            continue

        # Trigger reparse
        if trigger_reparse(project_id, language):
            success_count += 1
            # Wait a bit between requests to avoid overwhelming the server
            time.sleep(2)
        else:
            failed_count += 1

    # Summary
    print("\n" + "=" * 60)
    print("Re-parsing triggered!")
    print("=" * 60)
    print(f"Total projects: {len(projects)}")
    print(f"Triggered: {success_count}")
    print(f"Failed: {failed_count}")
    print(f"Skipped: {skipped_count}")
    print("=" * 60)
    print("\nNote: Dependency parsing runs in background.")
    print("Wait 30-60 seconds then check the white_list table.")

if __name__ == "__main__":
    main()
