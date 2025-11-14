#!/usr/bin/env python3
"""
后端API完整测试脚本 - Python版本
支持Flask和Spring Boot API的完整测试
"""

import requests
import json
import sys
from urllib.parse import urlencode, quote
from datetime import datetime
from typing import Dict, List, Tuple, Any

class APITester:
    """API测试类"""

    def __init__(self, flask_url: str = "http://127.0.0.1:5000", springboot_url: str = "http://localhost:8081"):
        self.flask_url = flask_url
        self.springboot_url = springboot_url
        self.test_results: List[Dict[str, Any]] = []
        self.test_count = 0
        self.pass_count = 0

    def test_api(self, name: str, method: str, url: str, data: Dict = None,
                 expected_status: int = 200, headers: Dict = None) -> Tuple[bool, str]:
        """
        测试单个API

        Args:
            name: 测试名称
            method: HTTP方法 (GET, POST, etc)
            url: 完整URL
            data: 请求体数据
            expected_status: 期望的HTTP状态码
            headers: 自定义请求头

        Returns:
            (是否通过, 响应文本)
        """
        self.test_count += 1

        print(f"\n[测试 {self.test_count}] {name}")
        print(f"  方法: {method}")
        print(f"  URL: {url}")

        try:
            # 发送请求
            if method.upper() == "GET":
                response = requests.get(url, timeout=10, headers=headers)
            elif method.upper() == "POST":
                if headers and 'Content-Type' in headers and 'multipart' in headers['Content-Type']:
                    # multipart请求
                    response = requests.post(url, files=data, timeout=10, headers={k:v for k,v in headers.items() if k != 'Content-Type'})
                else:
                    # JSON请求
                    response = requests.post(url, json=data, timeout=10, headers=headers)
            else:
                print(f"  ✗ 不支持的HTTP方法: {method}")
                return False, "Unsupported method"

            status_code = response.status_code
            print(f"  状态码: {status_code}")

            # 检查状态码
            if status_code == expected_status or status_code == 200 or status_code == 202:
                print(f"  ✓ 通过")
                self.pass_count += 1
                passed = True
            else:
                print(f"  ✗ 失败 (期望: {expected_status}, 实际: {status_code})")
                passed = False

            # 尝试获取响应体
            try:
                response_text = response.json()
                response_str = json.dumps(response_text, ensure_ascii=False)[:150]
            except:
                response_str = response.text[:150]

            print(f"  响应: {response_str}...")

            # 记录测试结果
            self.test_results.append({
                "name": name,
                "passed": passed,
                "status": status_code,
                "url": url
            })

            return passed, response.text

        except requests.exceptions.ConnectionError:
            print(f"  ✗ 连接失败 (服务器离线)")
            self.test_results.append({
                "name": name,
                "passed": False,
                "status": "ConnectionError",
                "url": url
            })
            return False, "Connection error"
        except requests.exceptions.Timeout:
            print(f"  ✗ 请求超时")
            self.test_results.append({
                "name": name,
                "passed": False,
                "status": "Timeout",
                "url": url
            })
            return False, "Timeout"
        except Exception as e:
            print(f"  ✗ 错误: {str(e)}")
            self.test_results.append({
                "name": name,
                "passed": False,
                "status": "Exception",
                "url": url
            })
            return False, str(e)

    def run_flask_tests(self):
        """运行Flask接口测试"""
        print("\n" + "="*50)
        print("Flask API 测试")
        print("="*50)

        # 1. 健康检查
        print("\n--- 基础服务测试 ---")
        self.test_api(
            "Flask服务器状态检查",
            "GET",
            f"{self.flask_url}/vulnerabilities/test"
        )

        # 2. 漏洞数据库接口
        print("\n--- 漏洞数据库接口 ---")
        self.test_api("获取GitHub漏洞", "GET", f"{self.flask_url}/vulnerabilities/github")
        self.test_api("获取AVD漏洞", "GET", f"{self.flask_url}/vulnerabilities/avd")
        self.test_api("获取NVD漏洞", "GET", f"{self.flask_url}/vulnerabilities/nvd")

        # 3. LLM查询接口
        print("\n--- LLM查询接口 ---")
        self.test_api(
            "LLM查询 (Qwen模型)",
            "GET",
            f"{self.flask_url}/llm/query?query=What%20is%20XSS&model=qwen"
        )
        self.test_api(
            "LLM查询 (DeepSeek模型)",
            "GET",
            f"{self.flask_url}/llm/query?query=How%20to%20prevent%20SQL%20injection&model=deepseek"
        )

        # 4. 漏洞修复建议
        print("\n--- 漏洞修复建议接口 (异步) ---")
        repair_data = {
            "vulnerability_name": "XSS",
            "vulnerability_desc": "Cross-site Scripting",
            "model": "qwen"
        }
        passed, response = self.test_api(
            "提交修复建议任务",
            "POST",
            f"{self.flask_url}/llm/repair/suggestion",
            data=repair_data,
            expected_status=202,
            headers={"Content-Type": "multipart/form-data"}
        )

        if passed:
            try:
                result = json.loads(response)
                task_id = result.get("task_id")
                if task_id:
                    self.test_api(
                        "查询任务状态",
                        "GET",
                        f"{self.flask_url}/llm/repair/suggestion/status/{task_id}"
                    )
                    self.test_api(
                        "获取任务结果",
                        "GET",
                        f"{self.flask_url}/llm/repair/suggestion/result/{task_id}"
                    )
            except:
                pass

        # 5. 语言检测
        print("\n--- 语言检测接口 ---")
        self.test_api(
            "检测主要语言 (无效路径)",
            "GET",
            f"{self.flask_url}/parse/get_primary_language?project_folder=/nonexistent"
        )
        self.test_api(
            "检测所有语言 (无效路径)",
            "GET",
            f"{self.flask_url}/parse/detect_languages?project_folder=/nonexistent"
        )

        # 6. 代码解析接口
        print("\n--- 代码解析接口 ---")
        parse_endpoints = [
            ("POM解析 (Java)", "/parse/pom_parse"),
            ("Go解析", "/parse/go_parse"),
            ("Python解析", "/parse/python_parse"),
            ("JavaScript解析", "/parse/javascript_parse"),
            ("PHP解析", "/parse/php_parse"),
            ("Ruby解析", "/parse/ruby_parse"),
            ("Rust解析", "/parse/rust_parse"),
            ("Erlang解析", "/parse/erlang_parse"),
        ]

        for name, endpoint in parse_endpoints:
            self.test_api(
                f"{name} (无效路径)",
                "GET",
                f"{self.flask_url}{endpoint}?project_folder=/nonexistent"
            )

        # 7. 漏洞检测
        print("\n--- 漏洞检测接口 ---")
        vuln_data = {
            "dependencies": [
                {"name": "log4j-core", "version": "2.14.1"}
            ]
        }
        self.test_api(
            "检测依赖漏洞",
            "POST",
            f"{self.flask_url}/vulnerabilities/detect",
            data=vuln_data
        )

    def run_springboot_tests(self):
        """运行Spring Boot接口测试"""
        print("\n" + "="*50)
        print("Spring Boot API 测试")
        print("="*50)

        # 1. 用户接口
        print("\n--- 用户管理接口 ---")
        login_data = {"username": "admin", "password": "admin123"}
        self.test_api(
            "用户登录",
            "POST",
            f"{self.springboot_url}/user/login",
            data=login_data
        )

        register_data = {
            "username": "testuser",
            "email": "test@example.com",
            "password": "Test123",
            "phone": "13800000000"
        }
        self.test_api(
            "用户注册",
            "POST",
            f"{self.springboot_url}/user/register",
            data=register_data
        )

        # 2. 项目接口
        print("\n--- 项目管理接口 ---")
        self.test_api(
            "获取项目列表",
            "GET",
            f"{self.springboot_url}/project"
        )

    def print_summary(self):
        """打印测试摘要"""
        print("\n" + "="*50)
        print("测试摘要")
        print("="*50)
        print(f"总测试数: {self.test_count}")
        print(f"通过数: {self.pass_count}")
        print(f"失败数: {self.test_count - self.pass_count}")

        if self.test_count > 0:
            percentage = (self.pass_count * 100) // self.test_count
            print(f"通过率: {percentage}%")

        print("\n详细结果:")
        for i, result in enumerate(self.test_results, 1):
            status = "✓ 通过" if result["passed"] else "✗ 失败"
            print(f"  {i}. {status} - {result['name']}")

        print(f"\n测试完成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    def run_all_tests(self):
        """运行所有测试"""
        print("\n" + "="*50)
        print("后端API完整测试")
        print(f"开始时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("="*50)

        self.run_flask_tests()
        self.run_springboot_tests()
        self.print_summary()

        return self.pass_count, self.test_count


def main():
    """主函数"""
    # 配置
    flask_url = "http://127.0.0.1:5000"
    springboot_url = "http://localhost:8081"

    # 创建测试器
    tester = APITester(flask_url, springboot_url)

    # 运行测试
    passed, total = tester.run_all_tests()

    # 返回退出码
    if passed == total:
        sys.exit(0)  # 全部通过
    else:
        sys.exit(1)  # 有失败


if __name__ == "__main__":
    main()
