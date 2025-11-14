#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
多语言依赖解析系统 - 快速测试脚本
测试所有语言的解析功能是否正常
"""

import requests
import json
import time

# 配置
BACKEND_URL = "http://localhost:8081"
FLASK_URL = "http://localhost:5000"
PROJECT_ID = 1  # 修改为实际的项目ID

# 测试统计
total_tests = 0
passed_tests = 0
failed_tests = 0

def test_language(language, display_name):
    """测试单一语言的解析"""
    global total_tests, passed_tests, failed_tests
    total_tests += 1

    print(f"\n{'='*50}")
    print(f"测试 {display_name} 项目解析")
    print('='*50)

    try:
        response = requests.post(
            f"{BACKEND_URL}/project/reparse",
            data={
                "projectId": PROJECT_ID,
                "language": language
            },
            timeout=10
        )

        result = response.json()

        if result.get("code") == 200:
            print(f"✓ {display_name} 解析请求成功")
            print(f"响应: {json.dumps(result, indent=2, ensure_ascii=False)}")
            passed_tests += 1
            return True
        else:
            print(f"✗ {display_name} 解析请求失败")
            print(f"响应: {json.dumps(result, indent=2, ensure_ascii=False)}")
            failed_tests += 1
            return False

    except Exception as e:
        print(f"✗ {display_name} 解析测试失败: {str(e)}")
        failed_tests += 1
        return False

def main():
    """主测试流程"""
    print("="*50)
    print("   多语言依赖解析系统 - 快速测试")
    print("="*50)

    # 检查Flask服务
    print("\n步骤 1: 检查Flask服务...")
    try:
        resp = requests.get(f"{FLASK_URL}/vulnerabilities/test", timeout=5)
        if resp.status_code == 200:
            print("✓ Flask服务正常运行 (Port 5000)")
        else:
            print(f"✗ Flask服务返回错误: {resp.status_code}")
            return
    except Exception as e:
        print(f"✗ Flask服务连接失败: {e}")
        print("  请启动Flask服务: python app.py")
        return

    # 检查Spring Boot服务
    print("\n步骤 2: 检查Spring Boot服务...")
    try:
        resp = requests.get(f"{BACKEND_URL}/project/info?projectid={PROJECT_ID}", timeout=5)
        if resp.status_code == 200:
            print("✓ Spring Boot服务正常运行 (Port 8081)")
        else:
            print(f"✗ Spring Boot服务返回错误: {resp.status_code}")
            return
    except Exception as e:
        print(f"✗ Spring Boot服务连接失败: {e}")
        print("  请启动Spring Boot服务")
        return

    # 测试各语言解析
    print("\n步骤 3: 测试各语言解析功能")

    languages = [
        ("java", "Java"),
        ("python", "Python"),
        ("go", "Go"),
        ("rust", "Rust"),
        ("javascript", "JavaScript"),
        ("php", "PHP"),
        ("ruby", "Ruby"),
        ("erlang", "Erlang"),
    ]

    for lang_code, lang_name in languages:
        test_language(lang_code, lang_name)
        time.sleep(0.5)  # 避免请求过快

    # 测试批量解析
    print("\n步骤 4: 测试批量解析功能")
    global total_tests, passed_tests, failed_tests
    total_tests += 1

    try:
        response = requests.post(
            f"{BACKEND_URL}/project/reparse/multiple",
            data={
                "projectId": PROJECT_ID,
                "languages": "java,python,go"
            },
            timeout=15
        )

        result = response.json()

        if result.get("code") == 200:
            print("✓ 批量解析请求成功")
            print(f"响应: {json.dumps(result, indent=2, ensure_ascii=False)}")
            passed_tests += 1
        else:
            print("✗ 批量解析请求失败")
            print(f"响应: {json.dumps(result, indent=2, ensure_ascii=False)}")
            failed_tests += 1

    except Exception as e:
        print(f"✗ 批量解析测试失败: {str(e)}")
        failed_tests += 1

    # 打印测试结果
    print("\n" + "="*50)
    print("测试结果汇总")
    print("="*50)

    print(f"\n总测试数: {total_tests}")
    print(f"通过: {passed_tests}")
    print(f"失败: {failed_tests}")

    if failed_tests == 0:
        print("\n╔════════════════════════════════════╗")
        print("║   所有测试通过！系统运行正常      ║")
        print("╚════════════════════════════════════╝")
        return 0
    else:
        print("\n╔════════════════════════════════════╗")
        print("║   部分测试失败，请检查错误日志    ║")
        print("╚════════════════════════════════════╝")
        return 1

if __name__ == "__main__":
    try:
        exit_code = main()
        exit(exit_code)
    except KeyboardInterrupt:
        print("\n\n测试被用户中断")
        exit(1)
    except Exception as e:
        print(f"\n测试过程中发生错误: {str(e)}")
        import traceback
        traceback.print_exc()
        exit(1)
