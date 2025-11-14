#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Rescan all projects to detect language and update database
"""

import requests
import pymysql
import urllib.parse
import os
import sys

# Configuration
MYSQL_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '15256785749rly',
    'database': 'kulin',
    'charset': 'utf8mb4'
}

FLASK_API = "http://localhost:5000/parse/get_primary_language"

def get_all_projects(conn):
    """Get all non-deleted projects"""
    cursor = conn.cursor()
    cursor.execute("SELECT id, name, file, language FROM project WHERE isdelete = 0 ORDER BY id")
    projects = cursor.fetchall()
    cursor.close()
    return projects

def detect_language(project_path):
    """Call Flask API to detect project language"""
    try:
        encoded_path = urllib.parse.quote(project_path)
        url = f"{FLASK_API}?project_folder={encoded_path}&use_optimized=true"

        print(f"  -> Calling Flask API...")
        response = requests.get(url, timeout=10)

        if response.status_code == 200:
            data = response.json()
            language = data.get('language', 'unknown')
            return language.lower() if language else 'unknown'
        else:
            print(f"  [ERROR] API returned status: {response.status_code}")
            return None
    except requests.exceptions.Timeout:
        print(f"  [ERROR] API timeout")
        return None
    except Exception as e:
        print(f"  [ERROR] API call failed: {e}")
        return None

def update_project_language(conn, project_id, new_language):
    """Update project language field"""
    cursor = conn.cursor()
    cursor.execute("UPDATE project SET language = %s WHERE id = %s", (new_language, project_id))
    conn.commit()
    cursor.close()

def main():
    print("=" * 60)
    print("Rescan project languages and update database")
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
    print(f"\nFound {len(projects)} projects to check\n")

    updated_count = 0
    failed_count = 0
    unchanged_count = 0

    for project in projects:
        project_id, name, file_path, current_language = project

        print("-" * 60)
        print(f"Project ID: {project_id}")
        print(f"Project Name: {name}")
        print(f"Current Language: {current_language}")
        print(f"Project Path: {file_path}")

        # Check if path exists
        if not os.path.isdir(file_path):
            print(f"  [WARNING] Path does not exist, skipping")
            failed_count += 1
            continue

        # Detect language
        detected_language = detect_language(file_path)

        if detected_language is None or detected_language == 'unknown':
            print(f"  [WARNING] Cannot detect language, keeping current: {current_language}")
            failed_count += 1
            continue

        print(f"  [OK] Detected language: {detected_language}")

        # Compare and update
        if detected_language != current_language:
            print(f"  [WARNING] Language mismatch! {current_language} -> {detected_language}")
            print(f"  -> Updating database...")

            try:
                update_project_language(conn, project_id, detected_language)
                print(f"  [OK] Updated successfully")
                updated_count += 1
            except Exception as e:
                print(f"  [ERROR] Update failed: {e}")
                failed_count += 1
        else:
            print(f"  [OK] Language correct, no update needed")
            unchanged_count += 1

    # Close database connection
    conn.close()

    # Summary
    print("\n" + "=" * 60)
    print("Scan complete!")
    print("=" * 60)
    print(f"Total projects: {len(projects)}")
    print(f"Updated: {updated_count}")
    print(f"Unchanged: {unchanged_count}")
    print(f"Failed/Skipped: {failed_count}")
    print("=" * 60)

    # Show updated project list
    conn = pymysql.connect(**MYSQL_CONFIG)
    cursor = conn.cursor()
    cursor.execute("SELECT id, name, language FROM project WHERE isdelete = 0 ORDER BY id")
    print("\nUpdated project list:")
    print(f"{'ID':<5} {'Language':<15} {'Project Name':<30}")
    print("-" * 60)
    for row in cursor.fetchall():
        print(f"{row[0]:<5} {row[2]:<15} {row[1]:<30}")
    cursor.close()
    conn.close()

if __name__ == "__main__":
    main()
