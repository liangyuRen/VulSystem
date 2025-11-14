@echo off
REM 快速诊断脚本 - 检查为什么数据没有写入white_list表

echo ========================================
echo   快速诊断：white_list数据写入问题
echo ========================================
echo.

REM 步骤1: 检查Flask服务
echo [1/5] 检查Flask服务...
curl -s http://localhost:5000/vulnerabilities/test >nul 2>&1
if %errorlevel%==0 (
    echo [成功] Flask服务运行正常
) else (
    echo [失败] Flask服务未运行！
    echo   请在flask目录运行: python app.py
    pause
    exit /b 1
)
echo.

REM 步骤2: 检查Spring Boot服务
echo [2/5] 检查Spring Boot服务...
curl -s http://localhost:8081/project/statistics?companyId=1 >nul 2>&1
if %errorlevel%==0 (
    echo [成功] Spring Boot服务运行正常
) else (
    echo [失败] Spring Boot服务未运行！
    echo   请运行: mvn spring-boot:run
    pause
    exit /b 1
)
echo.

REM 步骤3: 直接测试Flask Python解析API
echo [3/5] 测试Flask Python解析API...
echo   创建临时测试项目...

REM 创建临时目录和测试文件
set TEMP_DIR=%TEMP%\test-python-%RANDOM%
mkdir "%TEMP_DIR%"
echo requests==2.28.0 > "%TEMP_DIR%\requirements.txt"
echo flask==2.0.1 >> "%TEMP_DIR%\requirements.txt"
echo numpy==1.23.0 >> "%TEMP_DIR%\requirements.txt"

echo   测试项目路径: %TEMP_DIR%
echo.
echo   调用Flask API...

curl -s "http://localhost:5000/parse/python_parse?project_folder=%TEMP_DIR%" > temp_flask_response.txt

findstr /C:"name" temp_flask_response.txt >nul
if %errorlevel%==0 (
    echo [成功] Flask API返回了依赖数据
    echo   响应内容:
    type temp_flask_response.txt
) else (
    echo [失败] Flask API没有返回依赖数据
    echo   响应内容:
    type temp_flask_response.txt
)

del temp_flask_response.txt
rmdir /s /q "%TEMP_DIR%"
echo.

REM 步骤4: 检查Spring Boot日志
echo [4/5] 查看Spring Boot控制台日志
echo   请检查Spring Boot控制台是否有以下日志:
echo.
echo   ========================================
echo   开始解析PYTHON项目
echo   ========================================
echo   调用Flask API: ...
echo   成功解析出依赖库数量: X
echo   成功插入依赖库数量: X
echo.
pause

REM 步骤5: 检查数据库配置
echo.
echo [5/5] 数据库配置检查
echo   请确认以下配置:
echo.
echo   1. application.properties中的数据库连接配置
echo      spring.datasource.url=jdbc:mysql://localhost:3306/your_database
echo      spring.datasource.username=your_username
echo      spring.datasource.password=your_password
echo.
echo   2. white_list表是否存在
echo      执行SQL: SHOW TABLES LIKE 'white_list';
echo.
echo   3. white_list表结构是否正确
echo      执行SQL: DESCRIBE white_list;
echo.
pause

echo.
echo ========================================
echo   诊断建议
echo ========================================
echo.
echo 如果上传项目后没有数据写入white_list表，请检查:
echo.
echo 1. 上传的项目ZIP文件中是否包含依赖配置文件
echo    - Python项目: requirements.txt
echo    - Java项目: pom.xml
echo    - JavaScript项目: package.json
echo    - Go项目: go.mod
echo.
echo 2. 语言是否正确检测
echo    查看上传响应中的 "detectedLanguage" 字段
echo.
echo 3. 异步任务是否执行
echo    查看Spring Boot控制台日志
echo.
echo 4. 数据库连接是否正常
echo    检查application.properties配置
echo.
echo 5. Flask服务是否正常返回数据
echo    手动测试: curl "http://localhost:5000/parse/python_parse?project_folder=C:/test"
echo.
pause
