@echo off
REM 多语言项目依赖解析测试脚本 (Windows版本)
REM 用于测试所有支持语言的解析功能

echo =========================================
echo    多语言依赖解析系统测试
echo =========================================
echo.

REM 配置
set BACKEND_URL=http://localhost:8081
set FLASK_URL=http://localhost:5000
set PROJECT_ID=1

set total_tests=0
set passed_tests=0
set failed_tests=0

echo 步骤 1: 检查Flask服务状态
curl -s -o nul -w "%%{http_code}" "%FLASK_URL%/vulnerabilities/test" > temp_status.txt
set /p flask_status=<temp_status.txt
del temp_status.txt

if "%flask_status%"=="200" (
    echo [成功] Flask服务正常运行 (Port 5000)
) else (
    echo [失败] Flask服务未运行，请先启动Flask服务
    echo   启动命令: cd flask-service ^&^& python app.py
    exit /b 1
)

echo.
echo 步骤 2: 检查Spring Boot服务状态
curl -s -o nul -w "%%{http_code}" "%BACKEND_URL%/project/info?projectid=%PROJECT_ID%" > temp_status.txt
set /p backend_status=<temp_status.txt
del temp_status.txt

if "%backend_status%"=="200" (
    echo [成功] Spring Boot服务正常运行 (Port 8081)
) else (
    echo [失败] Spring Boot服务未运行或项目ID不存在
    echo   请检查服务状态和项目ID: %PROJECT_ID%
    exit /b 1
)

echo.
echo =========================================
echo 步骤 3: 测试各语言解析功能
echo =========================================

call :test_language "java" "Java"
call :test_language "python" "Python"
call :test_language "go" "Go"
call :test_language "rust" "Rust"
call :test_language "javascript" "JavaScript"
call :test_language "php" "PHP"
call :test_language "ruby" "Ruby"
call :test_language "erlang" "Erlang"
call :test_language "c" "C/C++"

echo.
echo =========================================
echo 步骤 4: 测试批量解析功能
echo =========================================

set /a total_tests+=1

curl -s -X POST "%BACKEND_URL%/project/reparse/multiple" -d "projectId=%PROJECT_ID%" -d "languages=java,python,go" > temp_response.txt
findstr /C:"\"code\":200" temp_response.txt >nul
if %errorlevel%==0 (
    echo [成功] 批量解析请求成功
    type temp_response.txt
    set /a passed_tests+=1
) else (
    echo [失败] 批量解析请求失败
    type temp_response.txt
    set /a failed_tests+=1
)
del temp_response.txt

echo.
echo =========================================
echo 测试结果汇总
echo =========================================
echo 总测试数: %total_tests%
echo 通过: %passed_tests%
echo 失败: %failed_tests%

if %failed_tests%==0 (
    echo.
    echo ╔════════════════════════════════════╗
    echo ║   所有测试通过！系统运行正常      ║
    echo ╚════════════════════════════════════╝
    exit /b 0
) else (
    echo.
    echo ╔════════════════════════════════════╗
    echo ║   部分测试失败，请检查错误日志    ║
    echo ╚════════════════════════════════════╝
    exit /b 1
)

:test_language
set language=%~1
set test_name=%~2

echo.
echo ----------------------------------------
echo 测试 %test_name% 项目解析
echo ----------------------------------------

set /a total_tests+=1

curl -s -X POST "%BACKEND_URL%/project/reparse" -d "projectId=%PROJECT_ID%" -d "language=%language%" > temp_response.txt
findstr /C:"\"code\":200" temp_response.txt >nul
if %errorlevel%==0 (
    echo [成功] %test_name% 解析请求成功
    type temp_response.txt
    set /a passed_tests+=1
) else (
    echo [失败] %test_name% 解析请求失败
    type temp_response.txt
    set /a failed_tests+=1
)
del temp_response.txt

goto :eof
