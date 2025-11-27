@echo off
REM 设置JDK路径
set JAVA_HOME=C:\Program Files\Java\jdk-17.0.1
set PATH=%JAVA_HOME%\bin;%PATH%

REM 验证JDK
echo.
echo ========== 验证Java环境 ==========
%JAVA_HOME%\bin\java -version
%JAVA_HOME%\bin\javac -version
echo.

REM 编译
echo ========== 开始编译 ==========
cd /d "C:\Users\任良玉\Desktop\kuling\VulSystem\backend"
call mvn clean compile -DskipTests

REM 检查编译结果
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========== 编译成功！==========
    echo.
) else (
    echo.
    echo ========== 编译失败！==========
    echo.
    exit /b 1
)
