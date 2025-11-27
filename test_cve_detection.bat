@echo off
REM Flask漏洞检测API - Windows测试脚本
REM 用于验证CVE-2021-45046在白名单中的检测

setlocal enabledelayedexpansion

echo.
echo ==========================================
echo Flask 漏洞检测 API 测试脚本
echo CVE-2021-45046 (Apache Log4j RCE)
echo ==========================================
echo.

REM 配置变量
set FLASK_URL=http://localhost:5000/vulnerabilities/detect
set SPRING_URL=http://localhost:8081/vulnerability/detect
set COMPANY_ID=1
set LANGUAGE=java

REM 测试1: Flask API 基础测试
echo [测试1] Flask API 基础功能测试
echo ===========================================
echo 请求: POST %FLASK_URL%
echo 数据: CVE-2021-45046 + 白名单匹配
echo.

curl -X POST "%FLASK_URL%" ^
  -H "Content-Type: application/json" ^
  -d "{^
    \"language\": \"java\",^
    \"detect_strategy\": \"LLM-whiteList\",^
    \"cve_id\": \"CVE-2021-45046\",^
    \"desc\": \"Apache Log4j2 RCE\",^
    \"company\": \"test\",^
    \"similarityThreshold\": 0.7,^
    \"white_list\": [^
      {\"name\": \"log4j-core\", \"language\": \"java\", \"projectId\": \"1\"},^
      {\"name\": \"log4j-api\", \"language\": \"java\", \"projectId\": \"1\"},^
      {\"name\": \"spring-boot-starter-web\", \"language\": \"java\", \"projectId\": \"1\"},^
      {\"name\": \"junit-jupiter\", \"language\": \"java\", \"projectId\": \"1\"},^
      {\"name\": \"commons-lang3\", \"language\": \"java\", \"projectId\": \"1\"},^
      {\"name\": \"redis.clients\", \"language\": \"java\", \"projectId\": \"1\"}^
    ]^
  }"

echo.
echo.

REM 测试2: Spring Boot 检测测试
echo [测试2] Spring Boot 端到端检测测试
echo ===========================================
echo 请求: POST %SPRING_URL%?companyId=%COMPANY_ID%^&language=%LANGUAGE%
echo.

curl -X POST "%SPRING_URL%?companyId=%COMPANY_ID%&language=%LANGUAGE%"

echo.
echo.

REM 测试3: 数据库验证
echo [测试3] 数据库验证
echo ===========================================
echo 检查数据库中的CVE-2021-45046记录...
echo.

mysql -h localhost -u root -p15256785749rly kulin -e "SELECT v.id, v.name, v.language, v.ref, v.riskLevel FROM vulnerability v WHERE v.ref = 'CVE-2021-45046' LIMIT 10;"

echo.
echo.

REM 测试4: 关联表验证
echo [测试4] 白名单和漏洞关联验证
echo ===========================================
echo 检查vulnerability_report_vulnerability关联表...
echo.

mysql -h localhost -u root -p15256785749rly kulin -e "SELECT vr.cve_id, v.name, v.ref FROM vulnerability_report_vulnerability vrv JOIN vulnerability_report vr ON vrv.vulnerability_report_id = vr.id JOIN vulnerability v ON vrv.vulnerability_id = v.id WHERE vr.cve_id = 'CVE-2021-45046' LIMIT 10;"

echo.
echo.

REM 最终总结
echo ==========================================
echo 测试完成
echo ==========================================
echo.
echo 关键发现:
echo   1. Flask API应该匹配: log4j-core, log4j-api
echo   2. Spring Boot应该保存记录到数据库
echo   3. 数据库应该有相应的关联记录
echo   4. 检测应该在2秒内完成
echo.
echo 如果测试失败,请检查:
echo   1. Flask服务是否运行在 http://localhost:5000
echo   2. Spring Boot服务是否运行在 http://localhost:8081
echo   3. MySQL数据库连接是否正常
echo   4. white_list中的组件名是否正确
echo.

pause
