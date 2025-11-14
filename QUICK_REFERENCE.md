# 漏洞检测系统快速参考指南

## 核心逻辑一页纸说明

### 数据流向
```
后端 (Spring Boot 8081)
  ↓ HTTP POST + JSON
Flask (5000)
  ↓ 接收请求
getLabels(params)
  ↓ 解析参数
tf_idf.py (检测算法)
  ├─ tiny_model_process_data_to_json()    [TF-IDF向量匹配]
  └─ llm_process_data_to_json()           [深度学习模型]
  ↓ 初步结果
threshold_cal.py (后处理过滤)
  ├─ Levenshtein 相似度
  ├─ Cosine 相似度
  └─ LCS 相似度
  ↓ 返回 "comp1;comp2;comp3"
后端 (Spring Boot)
  ↓ 保存到数据库
完成 ✓
```

---

## 关键文件位置

| 文件 | 位置 | 作用 |
|------|------|------|
| app.py | kulin/ | Flask主应用 |
| getLabels.py | kulin/VulLibGen/ | 参数解析和策略选择 |
| tf_idf.py | kulin/VulLibGen/tf_idf/ | 核心检测算法 |
| threshold_cal.py | kulin/VulLibGen/tf_idf/ | 相似度过滤 |
| VulnerabilityController.java | backend/src/main/java/com/nju/backend/controller/ | 后端API入口 |
| VulnerabilityDetectionService.java | backend/src/main/java/com/nju/backend/service/ | 检测服务逻辑 |

---

## 四种检测方式对比

### 1️⃣ TinyModel (默认，最快)
```
优点: ⚡ 速度快 (100ms)
      无外部依赖
缺点: ⭐⭐⭐ 准确度一般
使用: curl -X POST http://localhost:5000/vulnerabilities/detect \
       -H "Content-Type: application/json" \
       -d '{"detect_strategy":"TinyModel", ...}'
```

### 2️⃣ TinyModel-cos (推荐)
```
优点: ⚡⚡ 速度较快 (200ms)
      ⭐⭐⭐⭐⭐ 准确度最好 (cosine相似度)
缺点: 略慢
使用: {"detect_strategy":"TinyModel-cos", ...}
```

### 3️⃣ TinyModel-lev (编辑距离)
```
优点: ⭐⭐⭐⭐ 对组件名拼写敏感
缺点: 对描述匹配不如cosine
使用: {"detect_strategy":"TinyModel-lev", ...}
```

### 4️⃣ LLM (精准，但慢)
```
优点: ⭐⭐⭐⭐⭐⭐ 准确度最高 (语义理解)
缺点: 🐢 速度慢 (2-5秒)
      需要模型资源
使用: {"detect_strategy":"LLM", ...}
```

---

## 参数说明

### 必填参数
```python
{
    "cve_id": "CVE-2021-44228",              # CVE标识
    "desc": "漏洞描述文本...",                # 漏洞描述
    "language": "java",                      # java/c
    "white_list": "[{\"name\":...}]",        # JSON格式的组件列表
    "company": "公司名称",                    # 公司名
    "detect_strategy": "TinyModel-cos",      # 检测策略
    "similarityThreshold": 0.8               # 相似度阈值 (0.0-1.0)
}
```

### 相似度阈值推荐
```
- 0.5: 宽松模式 (多漏报)
- 0.7: 标准模式
- 0.8: 严格模式 (推荐)
- 0.95: 超严格模式 (多漏检)
```

---

## 响应格式

### 成功响应
```json
"log4j-core;log4j-api;commons-logging"
```

### 错误响应
```json
""  // 空字符串表示没有匹配
```

### 解析示例 (Java)
```java
String result = response.getBody();  // "log4j-core;log4j-api"
List<String> components = Arrays.asList(result.split(";"));
// components: ["log4j-core", "log4j-api"]
```

---

## API调用示例

### curl 测试
```bash
# 测试端点
curl -X POST http://localhost:5000/vulnerabilities/test

# 完整检测
curl -X POST http://localhost:5000/vulnerabilities/detect \
  -H "Content-Type: application/json" \
  -d '{
    "cve_id": "CVE-2024-TEST",
    "desc": "A vulnerability in Apache Log4j allows remote code execution",
    "language": "java",
    "white_list": "[{\"name\": \"log4j-core\", \"language\": \"java\", \"pojectid\": \"1\"}]",
    "company": "TestCorp",
    "detect_strategy": "TinyModel-cos",
    "similarityThreshold": 0.8
  }'
```

### Python 测试
```python
import requests
import json

url = "http://localhost:5000/vulnerabilities/detect"
payload = {
    "cve_id": "CVE-2024-TEST",
    "desc": "A vulnerability...",
    "language": "java",
    "white_list": json.dumps([{"name": "log4j-core"}]),
    "company": "TestCorp",
    "detect_strategy": "TinyModel-cos",
    "similarityThreshold": 0.8
}

response = requests.post(url, json=payload)
print(response.text)  # "log4j-core"
```

### Java 测试 (后端)
```java
@PostMapping("/vulnerability/detect")
public RespBean detectVulnerabilities(
    @RequestParam("companyId") int companyId,
    @RequestParam("language") String language) {

    List<VulnerabilityReport> reports = vulnerabilityReportMapper.selectList(null);
    Map<String, Object> result = vulnerabilityDetectionService
        .detectVulnerabilitiesForCompanyAndLanguage(
            companyId, language, reports
        );

    return RespBean.success(result);
}
```

---

## 数据库表关系

```
白名单 (white_list)
├─ id
├─ file_path (项目路径)
├─ name (组件名)
├─ language (java/c)
├─ description
└─ isdelete

                ↓ 匹配

漏洞 (vulnerability)
├─ id
├─ name (漏洞名)
├─ description (漏洞描述)
├─ ref (CVE ID)
├─ language (java/c)
├─ risk_level (风险等级)
└─ time (披露时间)

                ↓ 关联

漏洞-项目 (project_vulnerability)
├─ project_id
├─ vulnerability_id
└─ is_delete
```

---

## 常见问题快速解答

| 问题 | 答案 |
|------|------|
| 为什么没有匹配结果? | ① 相似度阈值太高 ② 组件库不完整 ③ 漏洞描述不够详细 |
| TinyModel 和 LLM 选哪个? | 对于大多数场景，TinyModel-cos 最平衡 |
| 如何提高准确度? | ① 调整阈值 ② 使用 LLM ③ 完善组件库 |
| 响应时间太长? | 使用 TinyModel (100ms) 而不是 LLM (2-5s) |
| 自己的组件库如何使用? | 替换 white_list JSON 参数即可 |

---

## 集成检查清单

- [ ] Flask 服务运行在 5000 端口
- [ ] 后端可以访问 http://localhost:5000
- [ ] 数据库中有 vulnerability_report 数据
- [ ] 项目扫描已完成，white_list 有数据
- [ ] 选择合适的检测策略（推荐 TinyModel-cos）
- [ ] 设置合理的相似度阈值（推荐 0.8）
- [ ] 测试API端点能正常返回
- [ ] 检查数据库中漏洞记录是否正确保存

---

## 性能指标参考

| 操作 | 时间 |
|------|------|
| TinyModel 检测单个CVE | ~100ms |
| TinyModel-cos (加过滤) | ~200ms |
| TinyModel-lcs (加过滤) | ~500ms |
| LLM 检测单个CVE | 2-5s |
| 保存漏洞到数据库 | ~50ms |

**吞吐量**: 单线程约 5-10 CVE/秒 (TinyModel)

---

## 快速部署

### 1. 启动 Flask
```bash
cd C:\Users\任良玉\Desktop\kuling\kulin
source venv/Scripts/activate  # Windows: venv\Scripts\activate
python app.py
# 访问 http://localhost:5000/vulnerabilities/test 测试
```

### 2. 启动 Spring Boot
```bash
cd C:\Users\任良玉\Desktop\kuling\VulSystem\backend
mvn spring-boot:run
# 访问 http://localhost:8081/vulnerability/detect/all 测试
```

### 3. 验证
```bash
# 检查 Flask
curl http://localhost:5000/vulnerabilities/test

# 检查后端
curl http://localhost:8081/vulnerability/detect/all
```

---

## 下一步

1. 📊 监控检测准确率
2. 🔧 根据需要调整阈值
3. 📈 性能优化 (异步处理、缓存)
4. 🌐 部署到生产环境 (autodl)
5. 🔐 添加用户认证和权限控制

---

**版本**: 1.0
**最后更新**: 2025-11-14
**快速参考**: ✓ 一页纸搞定
