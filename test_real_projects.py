#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
使用数据库中已有的真实项目进行测试
"""

import requests
import json
import time
import mysql.connector
import sys

# ============== 配置（请根据实际情况修改） ==============
BACKEND_URL = "http://localhost:8081"
FLASK_URL = "http://localhost:5000"

# MySQL配置
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '15256785749rly',
    'database': 'kulin',
    'port': 3306,
    'charset': 'utf8mb4'
}

# ============== 颜色输出 ==============
try:
    from colorama import init, Fore, Style
    init(autoreset=True)
    GREEN = Fore.GREEN
    RED = Fore.RED
    YELLOW = Fore.YELLOW
    BLUE = Fore.CYAN
    RESET = Style.RESET_ALL
except ImportError:
    GREEN = RED = YELLOW = BLUE = RESET = ''

def success(msg):
    print(f"{GREEN}✓ {msg}{RESET}")

def error(msg):
    print(f"{RED}✗ {msg}{RESET}")

def warning(msg):
    print(f"{YELLOW}⚠ {msg}{RESET}")

def info(msg):
    print(f"{BLUE}ℹ {msg}{RESET}")

# ============== 数据库操作 ==============
def get_db_connection():
    """获取数据库连接"""
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        success("数据库连接成功")
        return conn
    except Exception as e:
        error(f"数据库连接失败: {e}")
        print(f"\n请检查DB_CONFIG配置:")
        print(f"  host: {DB_CONFIG['host']}")
        print(f"  database: {DB_CONFIG['database']}")
        print(f"  user: {DB_CONFIG['user']}")
        return None

def get_projects_from_db(conn):
    """从数据库获取所有项目"""
    cursor = conn.cursor(dictionary=True)
    try:
        query = """
            SELECT id, name, description, language, file as file_path
            FROM project
            WHERE is_delete = 0 OR isDelete = 0
            ORDER BY id DESC
        """
        cursor.execute(query)
        projects = cursor.fetchall()
        cursor.close()
        return projects
    except mysql.connector.Error as e:
        error(f"查询项目失败: {e}")
        # 尝试另一种表结构
        try:
            query = """
                SELECT id, name, description, language, file_path
                FROM project
                WHERE isdelete = 0
                ORDER BY id DESC
            """
            cursor.execute(query)
            projects = cursor.fetchall()
            cursor.close()
            return projects
        except Exception as e2:
            error(f"第二次查询也失败: {e2}")
            cursor.close()
            return []

def count_whitelist_by_project(conn, file_path, language):
    """统计指定项目的white_list记录数"""
    cursor = conn.cursor()
    try:
        query = """
            SELECT COUNT(*)
            FROM white_list
            WHERE file_path = %s AND language = %s AND isdelete = 0
        """
        cursor.execute(query, (file_path, language))
        count = cursor.fetchone()[0]
        cursor.close()
        return count
    except Exception as e:
        error(f"查询white_list失败: {e}")
        cursor.close()
        return 0

def get_whitelist_entries(conn, file_path, language, limit=5):
    """获取指定项目的white_list记录"""
    cursor = conn.cursor(dictionary=True)
    try:
        query = """
            SELECT id, name, language, description
            FROM white_list
            WHERE file_path = %s AND language = %s AND isdelete = 0
            ORDER BY id DESC
            LIMIT %s
        """
        cursor.execute(query, (file_path, language, limit))
        entries = cursor.fetchall()
        cursor.close()
        return entries
    except Exception as e:
        error(f"查询white_list记录失败: {e}")
        cursor.close()
        return []

# ============== 测试函数 ==============
def test_project_parsing(project, conn):
    """测试单个项目的解析"""
    project_id = project['id']
    project_name = project['name']
    language = project['language']
    file_path = project.get('file_path') or project.get('file')

    print("\n" + "="*70)
    print(f"测试项目: {project_name} (ID: {project_id})")
    print("="*70)

    info(f"项目ID: {project_id}")
    info(f"项目名称: {project_name}")
    info(f"语言类型: {language}")
    info(f"文件路径: {file_path}")

    # 检查项目路径是否存在
    import os
    if not os.path.exists(file_path):
        warning(f"项目路径不存在: {file_path}")
        return False

    # 查询解析前的依赖数量
    before_count = count_whitelist_by_project(conn, file_path, language)
    info(f"解析前white_list记录数: {before_count}")

    # 调用重新解析接口
    try:
        info("正在调用解析接口...")

        response = requests.post(
            f"{BACKEND_URL}/project/reparse",
            data={
                "projectId": project_id,
                "language": language
            },
            timeout=30
        )

        if response.status_code == 200:
            result = response.json()
            if result.get('code') == 200:
                success("解析请求已提交")
                print(f"响应: {json.dumps(result, indent=2, ensure_ascii=False)}")

                # 等待异步解析完成
                info("等待异步解析完成（15秒）...")
                for i in range(15):
                    time.sleep(1)
                    print(f"  等待中 {i+1}/15...", end='\r')
                print()

                # 查询解析后的依赖数量
                after_count = count_whitelist_by_project(conn, file_path, language)
                info(f"解析后white_list记录数: {after_count}")

                new_count = after_count - before_count

                if new_count > 0:
                    success(f"✓✓✓ 成功！新增了 {new_count} 条依赖记录到white_list表")

                    # 显示新增的依赖
                    entries = get_whitelist_entries(conn, file_path, language, 5)
                    if entries:
                        info("新增的依赖示例:")
                        for entry in entries:
                            print(f"  - {entry['name']} ({entry['language']})")

                    return True
                elif after_count > 0:
                    warning(f"没有新增记录，但已有 {after_count} 条记录（可能之前已解析过）")

                    # 显示已有的依赖
                    entries = get_whitelist_entries(conn, file_path, language, 5)
                    if entries:
                        info("已有的依赖示例:")
                        for entry in entries:
                            print(f"  - {entry['name']} ({entry['language']})")

                    return True
                else:
                    error("解析后仍然没有记录写入white_list表！")

                    # 诊断问题
                    warning("可能的原因:")
                    print("  1. Flask API返回了空结果")
                    print("  2. 项目中没有依赖配置文件")
                    print("  3. JSON解析失败")
                    print("  4. 数据库插入失败")

                    # 手动测试Flask API
                    info("正在手动测试Flask API...")
                    test_flask_api_directly(language, file_path)

                    return False
            else:
                error(f"解析请求失败: {result.get('message')}")
                return False
        else:
            error(f"HTTP错误: {response.status_code}")
            print(f"响应: {response.text[:500]}")
            return False

    except Exception as e:
        error(f"测试失败: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_flask_api_directly(language, file_path):
    """直接测试Flask API"""
    api_map = {
        'java': '/parse/pom_parse',
        'python': '/parse/python_parse',
        'go': '/parse/go_parse',
        'rust': '/parse/rust_parse',
        'javascript': '/parse/javascript_parse',
        'php': '/parse/php_parse',
        'ruby': '/parse/ruby_parse',
        'erlang': '/parse/erlang_parse',
        'c': '/parse/c_parse',
        'cpp': '/parse/c_parse',
        'c++': '/parse/c_parse'
    }

    endpoint = api_map.get(language.lower(), f'/parse/{language}_parse')
    url = f"{FLASK_URL}{endpoint}"

    try:
        info(f"测试Flask API: {url}")
        info(f"项目路径: {file_path}")

        response = requests.get(url, params={'project_folder': file_path}, timeout=30)

        if response.status_code == 200:
            try:
                data = response.json()
                if isinstance(data, list):
                    if len(data) > 0:
                        success(f"Flask API返回了 {len(data)} 个依赖")
                        info("示例依赖:")
                        for dep in data[:3]:
                            print(f"  - {dep.get('name', 'N/A')}")
                    else:
                        warning("Flask API返回空数组")
                elif isinstance(data, dict) and 'obj' in data:
                    deps = data['obj']
                    if len(deps) > 0:
                        success(f"Flask API返回了 {len(deps)} 个依赖")
                    else:
                        warning("Flask API返回空数组")
                else:
                    warning("Flask API返回格式异常")
                    print(f"响应: {json.dumps(data, indent=2, ensure_ascii=False)[:500]}")
            except Exception as e:
                error(f"JSON解析失败: {e}")
                print(f"响应内容: {response.text[:500]}")
        else:
            error(f"Flask API返回错误: {response.status_code}")
            print(f"响应: {response.text[:500]}")

    except Exception as e:
        error(f"Flask API测试失败: {e}")

def main():
    """主测试流程"""
    print("\n" + "="*70)
    print("  使用数据库中的真实项目进行解析测试")
    print("="*70)

    # 连接数据库
    conn = get_db_connection()
    if not conn:
        error("无法连接数据库，请检查DB_CONFIG配置")
        return

    # 获取所有项目
    projects = get_projects_from_db(conn)

    if not projects:
        warning("数据库中没有找到项目")
        info("请确认:")
        print("  1. 数据库名称是否正确")
        print("  2. project表是否存在")
        print("  3. project表中是否有数据")
        conn.close()
        return

    success(f"找到 {len(projects)} 个项目")

    # 显示项目列表
    print("\n项目列表:")
    print("-"*70)
    for i, proj in enumerate(projects, 1):
        file_path = proj.get('file_path') or proj.get('file')
        print(f"{i}. {proj['name']} (ID:{proj['id']}, 语言:{proj['language']}, 路径:{file_path[:50]}...)")
    print("-"*70)

    # 询问用户选择
    print("\n请选择要测试的项目:")
    print("  输入项目编号（1-{}）测试单个项目".format(len(projects)))
    print("  输入 'all' 测试所有项目")
    print("  输入 'q' 退出")

    choice = input("\n请输入: ").strip()

    if choice.lower() == 'q':
        print("退出测试")
        conn.close()
        return

    # 测试项目
    test_results = []

    if choice.lower() == 'all':
        for proj in projects:
            result = test_project_parsing(proj, conn)
            test_results.append((proj['name'], result))
    else:
        try:
            index = int(choice) - 1
            if 0 <= index < len(projects):
                proj = projects[index]
                result = test_project_parsing(proj, conn)
                test_results.append((proj['name'], result))
            else:
                error("无效的项目编号")
                conn.close()
                return
        except ValueError:
            error("无效的输入")
            conn.close()
            return

    # 显示测试结果
    print("\n" + "="*70)
    print("测试结果汇总")
    print("="*70)

    for proj_name, result in test_results:
        if result:
            print(f"{GREEN}✓{RESET} {proj_name}")
        else:
            print(f"{RED}✗{RESET} {proj_name}")

    # 显示white_list统计
    print("\n" + "="*70)
    print("white_list表统计")
    print("="*70)

    cursor = conn.cursor()
    try:
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
            for lang, count in results:
                print(f"  {lang:15s}: {count:5d} 条记录")
        else:
            warning("white_list表中没有数据")
    except Exception as e:
        error(f"查询统计失败: {e}")

    cursor.close()
    conn.close()

    print("\n" + "="*70)
    print("测试完成")
    print("="*70)

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n测试被用户中断")
        sys.exit(1)
    except Exception as e:
        error(f"\n测试过程中发生错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
