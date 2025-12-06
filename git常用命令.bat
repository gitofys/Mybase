@echo off
chcp 65001 >nul
echo ========================================
echo    Git 常用命令快速执行脚本
echo ========================================
echo.
echo 请选择要执行的操作：
echo.
echo 1. 查看当前状态
echo 2. 查看提交历史（简洁版）
echo 3. 查看提交历史（完整版）
echo 4. 查看所有标签
echo 5. 添加所有文件并提交
echo 6. 创建版本标签
echo 7. 查看文件变更
echo 8. 退出
echo.
set /p choice=请输入选项 (1-8): 

if "%choice%"=="1" (
    echo.
    echo === 当前状态 ===
    git status
    pause
    goto :eof
)

if "%choice%"=="2" (
    echo.
    echo === 提交历史（简洁版）===
    git log --oneline -10
    pause
    goto :eof
)

if "%choice%"=="3" (
    echo.
    echo === 提交历史（完整版）===
    git log -5
    pause
    goto :eof
)

if "%choice%"=="4" (
    echo.
    echo === 所有标签 ===
    git tag
    pause
    goto :eof
)

if "%choice%"=="5" (
    echo.
    echo === 添加并提交 ===
    git add .
    set /p message=请输入提交信息: 
    git commit -m "%message%"
    echo.
    echo 提交完成！
    pause
    goto :eof
)

if "%choice%"=="6" (
    echo.
    echo === 创建版本标签 ===
    set /p tag=请输入标签名（如 v2.18.17）: 
    set /p message=请输入标签说明: 
    git tag -a %tag% -m "%message%"
    echo.
    echo 标签创建完成！
    pause
    goto :eof
)

if "%choice%"=="7" (
    echo.
    echo === 文件变更 ===
    git diff
    pause
    goto :eof
)

if "%choice%"=="8" (
    exit
)

echo 无效选项！
pause
