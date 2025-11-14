#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
端到端测试：上传项目 -> 自动解析 -> 验证数据库写入
"""

import requests
import json
import time
import zipfile
import os
import tempfile
import mysql.connector
from pathlib import Path

# ============== 配置 ==============
BACKEND_URL = "http://localhost:8081"
FLASK_URL = "http://localhost:5000"

# MySQL配置（根据实际情况修改）
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'your_password',  # 修改为实际密码
    'database': 'vulsystem',      # 修改为实际数据库名
    'port': 3306
}

# 测试用公司ID
COMPANY_ID = 1

# ============== 颜色输出 ==============
class Colors:
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    END = '\033[0m'

def success(msg):
    print(f"{Colors.GREEN}✓ {msg}{Colors.END}")

def error(msg):
    print(f"{Colors.RED}✗ {msg}{Colors.END}")

def warning(msg):
    print(f"{Colors.YELLOW}⚠ {msg}{Colors.END}")

def info(msg):
    print(f"{Colors.BLUE}ℹ {msg}{Colors.END}")

# ============== 创建测试项目 ==============
def create_test_project(language, project_name):
    """创建测试项目并打包为ZIP"""
    temp_dir = tempfile.mkdtemp()
    project_dir = os.path.join(temp_dir, project_name)
    os.makedirs(project_dir, exist_ok=True)

    # 根据语言创建不同的依赖文件
    if language == 'python':
        config_file = os.path.join(project_dir, 'requirements.txt')
        with open(config_file, 'w') as f:
            f.write("""requests==2.28.0
flask==2.0.1
numpy==1.23.0
pandas==1.4.2
sqlalchemy==1.4.39
""")

    elif language == 'java':
        config_file = os.path.join(project_dir, 'pom.xml')
        with open(config_file, 'w') as f:
            f.write("""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.test</groupId>
    <artifactId>test-project</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.7.0</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.28</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.24</version>
        </dependency>
    </dependencies>
</project>
""")

    elif language == 'javascript':
        config_file = os.path.join(project_dir, 'package.json')
        with open(config_file, 'w') as f:
            f.write("""{
  "name": "test-project",
  "version": "1.0.0",
  "dependencies": {
    "express": "^4.18.0",
    "axios": "^1.4.0",
    "lodash": "^4.17.21",
    "moment": "^2.29.4",
    "mysql2": "^3.3.0"
  }
}
""")

    elif language == 'go':
        config_file = os.path.join(project_dir, 'go.mod')
        with open(config_file, 'w') as f:
            f.write("""module example.com/test-project

go 1.20

require (
    github.com/gin-gonic/gin v1.9.0
    github.com/go-sql-driver/mysql v1.7.0
    gorm.io/gorm v1.25.0
)
""")

    # 打包为ZIP
    zip_path = os.path.join(temp_dir, f'{project_name}.zip')
    with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(project_dir):
            for file in files:
                file_path = os.path.join(root, file)
                arcname = os.path.relpath(file_path, temp_dir)
                zipf.write(file_path, arcname)

    success(f"创建了测试{language}项目: {zip_path}")
    return zip_path

# ============== 数据库操作 ==============
def get_db_connection():
    """获取数据库连接"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        return conn
    except Exception as e:
        error(f"数据库连接失败: {e}")
        return None

def count_whitelist_entries(conn, language=None, file_path=None):
    """统计white_list表中的记录数"""
    cursor = conn.cursor()

    if language and file_path:
        query = "SELECT COUNT(*) FROM white_list WHERE language = %s AND file_path = %s AND isdelete = 0"
        cursor.execute(query, (language, file_path))
    elif language:
        query = "SELECT COUNT(*) FROM white_list WHERE language = %s AND isdelete = 0"
        cursor.execute(query, (language,))
    else:
        query = "SELECT COUNT(*) FROM white_list WHERE isdelete = 0"
        cursor.execute(query)

    count = cursor.fetchone()[0]
    cursor.close()
    return count

def get_whitelist_entries(conn, language, limit=10):
    """获取white_list表中的记录"""
    cursor = conn.cursor(dictionary=True)
    query = """
        SELECT id, name, language, file_path, description
        FROM white_list
        WHERE language = %s AND isdelete = 0
        ORDER BY id DESC
        LIMIT %s
    """
    cursor.execute(query, (language, limit))
    entries = cursor.fetchall()
    cursor.close()
    return entries

def clear_test_data(conn, file_path_pattern):
    """清理测试数据"""
    cursor = conn.cursor()
    query = "DELETE FROM white_list WHERE file_path LIKE %s"
    cursor.execute(query, (f'%{file_path_pattern}%',))
    conn.commit()
    deleted = cursor.rowcount
    cursor.close()
    if deleted > 0:
        info(f"清理了 {deleted} 条测试数据")
    return deleted

# ============== 测试步骤 ==============
def test_services():
    """步骤1: 检查服务状态"""
    print("\n" + "="*60)
    print("步骤 1: 检查服务状态")
    print("="*60)

    # 检查Flask
    try:
        resp = requests.get(f"{FLASK_URL}/vulnerabilities/test", timeout=5)
        if resp.status_code == 200:
            success("Flask服务正常运行 (Port 5000)")
        else:
            error(f"Flask服务异常: {resp.status_code}")
            return False
    except Exception as e:
        error(f"Flask服务连接失败: {e}")
        warning("请启动Flask服务: python app.py")
        return False

    # 检查Spring Boot
    try:
        resp = requests.get(f"{BACKEND_URL}/project/statistics?companyId={COMPANY_ID}", timeout=5)
        if resp.status_code == 200:
            success("Spring Boot服务正常运行 (Port 8081)")
        else:
            error(f"Spring Boot服务异常: {resp.status_code}")
            return False
    except Exception as e:
        error(f"Spring Boot服务连接失败: {e}")
        warning("请启动Spring Boot服务: mvn spring-boot:run")
        return False

    return True

def test_database_connection():
    """步骤2: 检查数据库连接"""
    print("\n" + "="*60)
    print("步骤 2: 检查数据库连接")
    print("="*60)

    conn = get_db_connection()
    if conn:
        success("数据库连接成功")

        # 检查white_list表是否存在
        cursor = conn.cursor()
        cursor.execute("SHOW TABLES LIKE 'white_list'")
        if cursor.fetchone():
            success("white_list表存在")

            # 显示表结构
            cursor.execute("DESCRIBE white_list")
            columns = cursor.fetchall()
            info("表结构:")
            for col in columns:
                print(f"  - {col[0]} ({col[1]})")
        else:
            error("white_list表不存在！")
            conn.close()
            return None

        cursor.close()
        return conn
    else:
        error("数据库连接失败")
        return None

def test_flask_api(language):
    """步骤3: 直接测试Flask API"""
    print("\n" + "="*60)
    print(f"步骤 3: 测试Flask {language.upper()} API")
    print("="*60)

    # 创建临时测试项目
    zip_path = create_test_project(language, f'test-{language}-project')

    # 解压到临时目录
    temp_dir = tempfile.mkdtemp()
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(temp_dir)

    project_path = temp_dir
    info(f"测试项目路径: {project_path}")

    # 调用Flask API
    api_endpoints = {
        'python': '/parse/python_parse',
        'java': '/parse/pom_parse',
        'javascript': '/parse/javascript_parse',
        'go': '/parse/go_parse'
    }

    endpoint = api_endpoints.get(language, f'/parse/{language}_parse')

    try:
        url = f"{FLASK_URL}{endpoint}"
        params = {'project_folder': project_path}

        info(f"调用Flask API: {url}")
        info(f"项目路径: {project_path}")

        resp = requests.get(url, params=params, timeout=30)

        if resp.status_code == 200:
            success("Flask API调用成功")

            try:
                data = resp.json()
                if isinstance(data, list):
                    info(f"返回了 {len(data)} 个依赖")
                    if len(data) > 0:
                        info("示例依赖:")
                        for dep in data[:3]:
                            print(f"  - {dep.get('name', 'N/A')}")
                    return True, project_path
                else:
                    warning("返回格式异常（不是数组）")
                    print(f"响应: {json.dumps(data, indent=2, ensure_ascii=False)[:500]}")
                    return False, project_path
            except Exception as e:
                error(f"JSON解析失败: {e}")
                print(f"响应内容: {resp.text[:500]}")
                return False, project_path
        else:
            error(f"Flask API调用失败: {resp.status_code}")
            print(f"响应: {resp.text[:500]}")
            return False, project_path

    except Exception as e:
        error(f"Flask API测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False, project_path

def test_upload_and_parse(language, conn):
    """步骤4: 上传项目并验证自动解析"""
    print("\n" + "="*60)
    print(f"步骤 4: 测试上传{language.upper()}项目并自动解析")
    print("="*60)

    # 创建测试项目
    zip_path = create_test_project(language, f'upload-test-{language}')

    # 记录上传前的依赖数量
    before_count = count_whitelist_entries(conn, language)
    info(f"上传前{language}依赖数量: {before_count}")

    # 上传项目
    try:
        with open(zip_path, 'rb') as f:
            files = {'file': (os.path.basename(zip_path), f, 'application/zip')}
            data = {
                'name': f'测试{language}项目',
                'description': f'端到端测试{language}项目',
                'companyId': COMPANY_ID
            }

            info("正在上传项目...")
            resp = requests.post(
                f"{BACKEND_URL}/project/uploadProject",
                files=files,
                data=data,
                timeout=60
            )

            if resp.status_code == 200:
                result = resp.json()
                if result.get('code') == 200:
                    success("项目上传成功")
                    data = result.get('data', {})
                    detected_lang = data.get('detectedLanguage', 'unknown')
                    file_path = data.get('filePath', '')

                    info(f"检测到的语言: {detected_lang}")
                    info(f"项目路径: {file_path}")

                    if detected_lang.lower() != language.lower():
                        warning(f"语言检测不匹配！期望: {language}, 实际: {detected_lang}")

                    # 等待异步解析完成
                    info("等待异步解析完成（10秒）...")
                    for i in range(10):
                        time.sleep(1)
                        print(f"  {i+1}/10...", end='\r')
                    print()

                    # 检查数据库
                    after_count = count_whitelist_entries(conn, language)
                    new_entries = after_count - before_count

                    info(f"上传后{language}依赖数量: {after_count}")

                    if new_entries > 0:
                        success(f"✓✓✓ 成功！新增了 {new_entries} 个{language}依赖到white_list表！")

                        # 显示新增的依赖
                        entries = get_whitelist_entries(conn, language, 5)
                        if entries:
                            info("新增的依赖示例:")
                            for entry in entries:
                                print(f"  - ID:{entry['id']} | {entry['name']} | {entry['language']} | {entry['file_path'][:50]}...")

                        return True, file_path
                    else:
                        error(f"失败！没有新增{language}依赖到数据库")
                        warning("可能的原因:")
                        print("  1. 异步任务未执行")
                        print("  2. Flask API返回空结果")
                        print("  3. 数据库插入失败")
                        print("  4. 语言检测错误")
                        return False, file_path
                else:
                    error(f"上传失败: {result.get('message')}")
                    return False, None
            else:
                error(f"HTTP错误: {resp.status_code}")
                print(f"响应: {resp.text[:500]}")
                return False, None

    except Exception as e:
        error(f"上传测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False, None

def main():
    """主测试流程"""
    print("\n" + "="*60)
    print("  端到端测试：上传项目 -> 自动解析 -> 验证数据库")
    print("="*60)

    # 步骤1: 检查服务
    if not test_services():
        error("服务检查失败，请确保Flask和Spring Boot服务都在运行")
        return False

    # 步骤2: 检查数据库
    conn = test_database_connection()
    if not conn:
        error("数据库连接失败，请检查配置")
        return False

    # 步骤3: 直接测试Flask API
    test_languages = ['python', 'java', 'javascript', 'go']

    for lang in test_languages:
        flask_ok, test_path = test_flask_api(lang)
        if not flask_ok:
            warning(f"Flask {lang} API测试失败，跳过该语言的上传测试")
            continue

        # 步骤4: 上传并验证
        upload_ok, file_path = test_upload_and_parse(lang, conn)

        if upload_ok:
            success(f"✓✓✓ {lang.upper()}项目端到端测试通过！")
        else:
            error(f"✗✗✗ {lang.upper()}项目端到端测试失败！")

        print("\n" + "-"*60 + "\n")

    # 最终统计
    print("\n" + "="*60)
    print("最终数据库统计")
    print("="*60)

    cursor = conn.cursor()
    cursor.execute("""
        SELECT language, COUNT(*) as count
        FROM white_list
        WHERE isdelete = 0
        GROUP BY language
        ORDER BY count DESC
    """)

    results = cursor.fetchall()
    if results:
        info("各语言依赖统计:")
        for row in results:
            print(f"  {row[0]:15s}: {row[1]:5d} 个依赖")
    else:
        warning("数据库中没有依赖记录")

    cursor.close()
    conn.close()

    print("\n" + "="*60)
    print("测试完成")
    print("="*60)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n测试被用户中断")
        exit(1)
    except Exception as e:
        error(f"测试过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
        exit(1)
