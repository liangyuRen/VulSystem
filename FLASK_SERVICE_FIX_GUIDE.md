# Flask 服务修复指南

## 问题概述

后端在调用 Flask 服务的 `/vulnerabilities/detect` 接口时出现以下错误：

1. **500 Internal Server Error** - Flask 服务返回服务器内部错误
2. **UnicodeEncodeError** - Python 中出现字符编码错误：`'gbk' codec can't encode character '\xf6'`
3. **404 Not Found** - 外部 API 调用失败，返回 404

## 错误链追踪

```
VulnerabilityJobHandler.detectVulnerabilities()
  → Flask: getLabels()
    → Flask: tf_idf.llm_process_data_to_json()
      → tf_idf.py:135: print("real_test" + real_test)  // UnicodeEncodeError
      → tf_idf.py:151: 外部 API 调用                    // 404 Not Found
```

## 需要修复的问题

### 1. Python Unicode 编码问题 (tf_idf.py:135)

**问题原因：**
- Python 字符串包含特殊 Unicode 字符（如 `\xf6`）
- 在 GBK 编码环境下无法正确处理
- 使用 `print()` 直接输出包含这些字符的字符串

**修复方案：**

```python
# 修复前（有问题的代码）
print("real_test" + real_test)
print("top10_real_test" + top10_real_test)

# 修复方案 1：使用 logging 模块（推荐）
import logging
logger = logging.getLogger(__name__)
logger.info(f"real_test length: {len(real_test)}")
logger.debug("Processed test data successfully")

# 修复方案 2：指定编码输出
import sys
import io
if sys.stdout.encoding != 'utf-8':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
print("real_test: " + real_test)

# 修复方案 3：移除调试输出
# 直接删除这两行，在 logging 中添加对应的日志
```

### 2. 外部 API 404 错误 (tf_idf.py:151)

**问题原因：**
- 外部 API 服务不可用或网络连接失败
- API 端点 URL 错误或已更改
- API 服务部署地址有变化

**修复方案：**

```python
# 修复前
response = requests.post(url, json=payload)
if response.status_code != 200:
    raise Exception(f"API请求失败，状态码: {response.status_code}, 响应内容: {response.text}")

# 修复后：添加重试逻辑和更好的错误处理
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

def create_session_with_retry():
    """创建带重试机制的请求会话"""
    session = requests.Session()
    retry_strategy = Retry(
        total=3,
        backoff_factor=1,
        status_forcelist=[429, 500, 502, 503, 504],
        allowed_methods=["HEAD", "GET", "OPTIONS", "POST"]
    )
    adapter = HTTPAdapter(max_retries=retry_strategy)
    session.mount("http://", adapter)
    session.mount("https://", adapter)
    return session

def call_external_api(url, payload, max_retries=3):
    """
    调用外部 API，包含重试逻辑和异常处理
    """
    session = create_session_with_retry()

    for attempt in range(max_retries):
        try:
            response = session.post(url, json=payload, timeout=30)
            response.raise_for_status()  # 在状态码 >= 400 时抛出异常
            return response.json()
        except requests.exceptions.Timeout:
            logging.warning(f"API 调用超时（第 {attempt+1} 次），URL: {url}")
            if attempt == max_retries - 1:
                raise Exception(f"API 调用在 {max_retries} 次重试后仍然超时")
        except requests.exceptions.ConnectionError as e:
            logging.warning(f"网络连接失败（第 {attempt+1} 次），URL: {url}: {e}")
            if attempt == max_retries - 1:
                raise Exception(f"无法连接到外部 API，URL: {url}")
        except requests.exceptions.HTTPError as e:
            logging.error(f"HTTP 错误（状态码 {response.status_code}）: {response.text[:200]}")
            if response.status_code == 404:
                raise Exception(f"API 端点不存在（404）。请检查 URL: {url}")
            elif response.status_code >= 500:
                logging.warning(f"API 服务器错误（第 {attempt+1} 次），将重试...")
                if attempt < max_retries - 1:
                    continue
            raise Exception(f"API 调用失败，状态码: {response.status_code}")
        except Exception as e:
            logging.error(f"未预期的错误：{type(e).__name__}: {e}")
            raise

    raise Exception("无法从 API 获取数据")
```

### 3. 改进的 Flask 路由错误处理

```python
# 在 app.py 或相应的蓝图中
from flask import jsonify
import logging
import traceback

logger = logging.getLogger(__name__)

@app.route('/vulnerabilities/detect', methods=['POST'])
def detect_vulnerabilities():
    try:
        params = request.json or request.form

        # 验证必要参数
        required_params = ['cve_id', 'desc', 'white_list', 'language']
        for param in required_params:
            if param not in params:
                return jsonify({
                    'error': f'缺少必需参数: {param}'
                }), 400

        # 处理数据
        data = getLabels(params=params)

        return jsonify({
            'success': True,
            'data': data
        }), 200

    except UnicodeEncodeError as e:
        logger.error(f"Unicode 编码错误: {e}", exc_info=True)
        return jsonify({
            'error': '数据编码错误，请确保输入数据正确',
            'details': str(e)
        }), 400

    except Exception as e:
        logger.error(f"处理请求时出错: {e}", exc_info=True)
        return jsonify({
            'error': '处理请求时出错',
            'details': str(e),
            'traceback': traceback.format_exc() if app.debug else None
        }), 500

# 全局错误处理器
@app.errorhandler(Exception)
def handle_error(error):
    logger.error(f"未处理的异常: {error}", exc_info=True)
    return jsonify({
        'error': '服务器内部错误',
        'message': str(error) if app.debug else '请稍后重试'
    }), 500
```

## 检查清单

在修复 Flask 服务时，请确认以下事项：

### 环境配置
- [ ] 确保 Python 文件使用 UTF-8 编码保存（在文件头添加 `# -*- coding: utf-8 -*-`）
- [ ] 检查环境变量 `PYTHONIOENCODING=utf-8` 是否设置
- [ ] 验证 Flask 应用的字符编码配置

### 依赖项
- [ ] 检查是否安装了所有必需的 Python 包
- [ ] 更新过期的依赖（如 `requests`, `Flask` 等）
- [ ] 验证 `requirements.txt` 中的版本

### API 端点
- [ ] 验证外部 API 的 URL 是否正确和可访问
- [ ] 检查 API 认证信息（如果需要）
- [ ] 测试 API 网络连接：`curl -v "https://api-url"`

### 日志配置
- [ ] 配置适当的日志级别和格式
- [ ] 确保日志能够正确编码和输出 Unicode 字符
- [ ] 添加请求/响应日志用于调试

### 测试步骤

1. **单元测试** - 测试各个函数的编码处理
```python
def test_unicode_handling():
    test_string = "测试包含特殊字符 \xf6"
    result = safe_print(test_string)
    assert result is not None
```

2. **集成测试** - 测试完整流程
```bash
curl -X POST http://localhost:5000/vulnerabilities/detect \
  -H "Content-Type: application/json" \
  -d '{
    "cve_id": "CVE-2024-1234",
    "desc": "测试漏洞描述",
    "white_list": "[]",
    "language": "java",
    "company": "test",
    "detect_strategy": "exact",
    "similarityThreshold": "0.8"
  }'
```

3. **压力测试** - 验证并发处理能力

## 后端 Java 改进

后端 `VulnerabilityJobHandler.java` 已更新，改进内容包括：

### 新增异常处理
- `HttpServerErrorException` - 处理 Flask 500 错误，记录详细错误信息
- `HttpClientErrorException` - 处理 404 和其他 4xx 错误
- `RestClientException` - 处理网络连接和超时错误
- 通用 `Exception` - 捕获其他未预期的异常

### 日志改进
- 更详细的 CVE 和公司信息
- 错误响应体的摘要记录
- 异常类型和堆栈跟踪输出

### 恢复机制
- 错误发生时跳过当前公司而不是中断整个批处理
- 继续处理下一个公司/语言组合

## 常见问题

### Q: UnicodeEncodeError 一直出现
**A:**
1. 检查 Python 文件编码：添加 `# -*- coding: utf-8 -*-`
2. 设置环境变量：`export PYTHONIOENCODING=utf-8`
3. 避免直接 print Unicode 字符，使用 logging 模块

### Q: Flask 返回 404
**A:**
1. 验证 API URL 是否正确
2. 检查外部服务是否在线
3. 检查网络连接和防火墙

### Q: 后端日志中看不到 Flask 的详细错误
**A:**
1. 检查 Flask 日志级别配置
2. 确保日志文件有写入权限
3. 查看 Flask 应用的标准输出和错误输出

## 联系与支持

如需进一步协助，请提供：
1. Flask 应用的日志文件（至少 100 行）
2. 后端 Java 日志中的完整错误堆栈
3. 网络连接诊断（如 `ping`, `traceroute` 结果）
