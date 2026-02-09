@echo off

REM Absolute path to project
set PROJECT_DIR=%~dp0
set PROJECT_DIR=%PROJECT_DIR:~0,-1%

REM Force swagger-ui as working directory
pushd %PROJECT_DIR%\swagger-ui

REM Isolated PATH (does NOT include existing PATH)
set PATH=%PROJECT_DIR%\..\node;%PROJECT_DIR%\swagger-ui\node_modules\.bin;%GIT_HOME%;%VS_CODE_HOME%\bin
echo Using isolated Node environment:
call node -v
call npm -v

REM Start  cmd shell in swagger-ui


cmd /K
