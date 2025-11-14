# 立即开始测试 - 使用数据库中的真实项目

## 🚀 快速开始

### 步骤1: 修改数据库配置

编辑 `test_real_projects.py` 文件，修改第9-16行的数据库配置：

```python
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': 'your_password',  # ← 改为你的MySQL密码
    'database': 'vul_system',      # ← 改为你的数据库名
    'port': 3306,
    'charset': 'utf8mb4'
}
```

### 步骤2: 确保服务运行

#### 启动Flask服务
```bash
python app.py
```

验证：访问 http://localhost:5000/vulnerabilities/test

#### 启动Spring Boot服务
```bash
cd backend
mvn spring-boot:run
```

验证：访问 http://localhost:8081

### 步骤3: 运行测试脚本

```bash
# 安装依赖（如果还没安装）
pip install mysql-connector-python requests colorama

# 运行测试
python test_real_projects.py
```

### 步骤4: 选择项目测试

脚本会显示数据库中的所有项目，例如：

```
项目列表:
----------------------------------------------------------------------
1. Java测试项目 (ID:1, 语言:java, 路径:C:/test/java-project...)
2. Python项目 (ID:2, 语言:python, 路径:C:/test/python-project...)
3. Go项目 (ID:3, 语言:go, 路径:C:/test/go-project...)
----------------------------------------------------------------------

请选择要测试的项目:
  输入项目编号（1-3）测试单个项目
  输入 'all' 测试所有项目
  输入 'q' 退出

请输入:
```

输入项目编号或 `all`，脚本会：
1. 调用 `/project/reparse` 接口触发解析
2. 等待15秒让异步任务完成
3. 查询white_list表查看是否有新数据
4. 显示新增的依赖记录

### 预期结果

成功的输出应该类似：

```
========================================
测试项目: Python项目 (ID: 2)
========================================
ℹ 项目ID: 2
ℹ 项目名称: Python项目
ℹ 语言类型: python
ℹ 文件路径: C:/test/python-project
ℹ 解析前white_list记录数: 0
ℹ 正在调用解析接口...
✓ 解析请求已提交
响应: {
  "code": 200,
  "message": "success",
  "data": {
    "status": "parsing",
    "message": "已触发python项目依赖解析，正在后台处理...",
    "language": "python",
    "projectId": 2
  }
}
ℹ 等待异步解析完成（15秒）...
ℹ 解析后white_list记录数: 15
✓✓✓ 成功！新增了 15 条依赖记录到white_list表
ℹ 新增的依赖示例:
  - requests (python)
  - flask (python)
  - numpy (python)
  - pandas (python)
  - sqlalchemy (python)
```

---

## 🔍 如果没有数据写入，请检查

### 检查1: Spring Boot日志

在Spring Boot控制台应该看到：

```
========================================
开始解析PYTHON项目
项目路径: C:/test/python-project
========================================
→ 调用Flask API: http://localhost:5000/parse/python_parse
✓ API响应接收成功，长度: 1234 字符
✓ 成功解析出依赖库数量: 15
========================================
✓ PYTHON项目解析完成
  总依赖数: 15
  成功插入: 15
  重复跳过: 0
  插入失败: 0
  耗时: 523 ms
========================================
```

如果没有这些日志：
- 异步任务可能没有执行
- 检查 `@EnableAsync` 是否配置
- 检查线程池配置

### 检查2: Flask API响应

脚本会自动测试Flask API，显示：

```
ℹ 测试Flask API: http://localhost:5000/parse/python_parse
ℹ 项目路径: C:/test/python-project
✓ Flask API返回了 15 个依赖
ℹ 示例依赖:
  - requests
  - flask
  - numpy
```

如果返回空数组：
- 项目中可能没有依赖配置文件
- 配置文件格式可能不正确
- 检查项目路径是否正确

### 检查3: 数据库连接

如果报错 "数据库连接失败"：
- 检查MySQL服务是否运行
- 检查DB_CONFIG中的配置是否正确
- 检查用户名和密码

### 检查4: 项目路径

确保数据库中的项目路径是正确的：

```sql
SELECT id, name, language, file FROM project WHERE is_delete = 0;
```

路径应该指向实际存在的目录。

---

## 📊 查看数据库数据

测试完成后，可以在MySQL中查询：

```sql
-- 查看所有依赖统计
SELECT language, COUNT(*) as count
FROM white_list
WHERE isdelete = 0
GROUP BY language;

-- 查看Python项目的依赖
SELECT id, name, language, file_path, description
FROM white_list
WHERE language = 'python' AND isdelete = 0
LIMIT 10;

-- 查看特定项目的依赖
SELECT id, name, language
FROM white_list
WHERE file_path = 'C:/test/python-project' AND isdelete = 0;
```

---

## ⚡ 快速诊断

如果遇到问题，运行诊断脚本：

```bash
diagnose.bat
```

这会检查：
- Flask服务状态
- Spring Boot服务状态
- Flask API响应
- 数据库配置

---

## 📝 常见问题

### Q1: 解析请求成功，但没有数据写入

**原因**: 异步任务可能失败了

**解决**:
1. 查看Spring Boot控制台日志
2. 检查是否有异常堆栈
3. 确认Flask服务正常运行

### Q2: Flask API返回空数组

**原因**: 项目中没有依赖配置文件

**解决**:
1. 检查项目目录是否包含：
   - Python: requirements.txt, setup.py, Pipfile
   - Java: pom.xml, build.gradle
   - Go: go.mod
   - JavaScript: package.json
2. 检查配置文件格式是否正确

### Q3: 数据库连接失败

**原因**: 数据库配置错误

**解决**:
1. 确认MySQL服务运行中
2. 检查数据库名、用户名、密码
3. 测试连接：`mysql -u root -p`

### Q4: 项目路径不存在

**原因**: 数据库中的路径已失效

**解决**:
1. 更新数据库中的项目路径：
   ```sql
   UPDATE project SET file = 'C:/new/path' WHERE id = 1;
   ```
2. 或使用uploadProject接口重新上传项目

---

## ✅ 成功标志

测试成功的标志：
- [x] 解析请求返回 code: 200
- [x] Spring Boot日志显示解析完成
- [x] white_list表中有新记录
- [x] 记录的language字段正确
- [x] 记录的file_path字段正确
- [x] 记录的name字段包含依赖名称

---

**立即运行 `python test_real_projects.py` 开始测试！**
