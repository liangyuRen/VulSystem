#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
测试字符编码修复 - 验证中文和特殊字符能否正确存储
"""

import pymysql
import json

MYSQL_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '15256785749rly',
    'database': 'kulin',
    'charset': 'utf8mb4'  # 使用 utf8mb4
}

def test_vulnerability_report_insertion():
    """测试插入带中文的漏洞报告"""
    print("=" * 70)
    print("测试字符编码修复 - vulnerability_report 表")
    print("=" * 70)

    conn = pymysql.connect(**MYSQL_CONFIG)
    cursor = conn.cursor()

    # 测试数据：包含中文、英文、特殊字符
    test_data = {
        'cve_id': 'TEST-2025-UTF8MB4',
        'vulnerability_name': '测试漏洞：Spring框架远程代码执行漏洞（RCE）',
        'description': '这是一个测试漏洞，用于验证UTF-8字符编码。包含中文、English、数字123、特殊符号©™®、emoji😀',
        'riskLevel': 'HIGH',
        'referenceLink': 'https://test.example.com/中文路径/测试',
        'disclosure_time': '2025-01-14 00:00:00',
        'affects_whitelist': 0,
        'isdelete': 0
    }

    try:
        # 1. 删除可能存在的测试记录
        cursor.execute("DELETE FROM vulnerability_report WHERE cve_id = %s", (test_data['cve_id'],))
        conn.commit()

        # 2. 插入测试数据
        print("\n插入测试数据...")
        insert_sql = """
        INSERT INTO vulnerability_report
        (cve_id, vulnerability_name, description, riskLevel, referenceLink, disclosure_time, affects_whitelist, isdelete)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
        """
        cursor.execute(insert_sql, (
            test_data['cve_id'],
            test_data['vulnerability_name'],
            test_data['description'],
            test_data['riskLevel'],
            test_data['referenceLink'],
            test_data['disclosure_time'],
            test_data['affects_whitelist'],
            test_data['isdelete']
        ))
        conn.commit()
        print("✓ 数据已插入")

        # 3. 读取并验证数据
        print("\n读取并验证数据...")
        cursor.execute("""
            SELECT cve_id, vulnerability_name, description, riskLevel, referenceLink
            FROM vulnerability_report
            WHERE cve_id = %s
        """, (test_data['cve_id'],))

        result = cursor.fetchone()

        if result:
            cve_id, vuln_name, desc, risk, ref = result

            print("\n" + "-" * 70)
            print("验证结果:")
            print("-" * 70)

            # 验证每个字段
            checks = [
                ('CVE ID', cve_id, test_data['cve_id']),
                ('漏洞名称', vuln_name, test_data['vulnerability_name']),
                ('描述', desc, test_data['description']),
                ('风险等级', risk, test_data['riskLevel']),
                ('参考链接', ref, test_data['referenceLink'])
            ]

            all_passed = True
            for field_name, actual, expected in checks:
                match = actual == expected
                status = "✓ PASS" if match else "✗ FAIL"
                print(f"\n{field_name}: {status}")
                print(f"  期望: {expected[:100]}{'...' if len(expected) > 100 else ''}")
                print(f"  实际: {actual[:100]}{'...' if len(actual) > 100 else ''}")

                if not match:
                    all_passed = False
                    print(f"  差异: {repr(expected)} != {repr(actual)}")

            print("\n" + "=" * 70)
            if all_passed:
                print("✓ 所有测试通过！字符编码修复成功！")
            else:
                print("✗ 部分测试失败！请检查数据库配置")
            print("=" * 70)

            # 4. 清理测试数据
            cursor.execute("DELETE FROM vulnerability_report WHERE cve_id = %s", (test_data['cve_id'],))
            conn.commit()
            print("\n✓ 测试数据已清理")

        else:
            print("✗ 无法读取插入的数据")

    except Exception as e:
        print(f"✗ 测试失败: {e}")
        import traceback
        traceback.print_exc()
    finally:
        cursor.close()
        conn.close()

def test_existing_data():
    """检查现有数据中的字符编码问题"""
    print("\n" + "=" * 70)
    print("检查现有数据的字符编码")
    print("=" * 70)

    conn = pymysql.connect(**MYSQL_CONFIG)
    cursor = conn.cursor()

    try:
        # 查询最近的10条记录
        cursor.execute("""
            SELECT id, cve_id, vulnerability_name,
                   LENGTH(vulnerability_name) as name_bytes,
                   CHAR_LENGTH(vulnerability_name) as name_chars
            FROM vulnerability_report
            ORDER BY id DESC
            LIMIT 10
        """)

        results = cursor.fetchall()

        print(f"\n最近 {len(results)} 条漏洞报告:")
        print("-" * 70)

        for row in results:
            id_, cve_id, vuln_name, name_bytes, name_chars = row

            # 检测是否有乱码（字节数远大于字符数可能是乱码）
            has_issue = name_bytes > name_chars * 3 or '��' in vuln_name or '�' in vuln_name
            status = "⚠ 可能乱码" if has_issue else "✓ 正常"

            print(f"\nID {id_} ({cve_id}): {status}")
            print(f"  名称: {vuln_name[:80]}{'...' if len(vuln_name) > 80 else ''}")
            print(f"  字节数: {name_bytes}, 字符数: {name_chars}")

    except Exception as e:
        print(f"✗ 查询失败: {e}")
    finally:
        cursor.close()
        conn.close()

if __name__ == "__main__":
    test_vulnerability_report_insertion()
    test_existing_data()

    print("\n" + "=" * 70)
    print("说明:")
    print("=" * 70)
    print("1. 新插入的数据应该能正确存储中文、emoji等字符")
    print("2. 旧数据中可能仍有乱码（已经损坏的数据无法自动恢复）")
    print("3. 如需修复旧数据，需要重新爬取漏洞信息")
    print("4. 后端服务器需要重启以使用新的数据库连接配置")
    print("=" * 70)
