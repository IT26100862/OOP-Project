@echo off
title EstateX - Frontend Server (Port 8080)
cd /d "%~dp0"
echo ========================================================
echo   Starting EstateX Frontend Server on Port 8080...
echo   URL: http://localhost:8080
echo ========================================================

REM 1. Check for standard python
where python >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [OK] Python detected. Starting web server on port 8080...
    timeout /t 1 >nul
    start http://localhost:8080
    python -m http.server 8080
    goto :end
)

REM 2. Check for Windows Python launcher (py)
where py >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [OK] Python launcher detected. Starting web server on port 8080...
    timeout /t 1 >nul
    start http://localhost:8080
    py -m http.server 8080
    goto :end
)

REM 3. Check for Node.js / npx
where npx >nul 2>&1
if %ERRORLEVEL% equ 0 (
    echo [OK] Node.js (npx) detected. Starting web server on port 8080...
    timeout /t 1 >nul
    start http://localhost:8080
    call npx serve -l 8080 .
    goto :end
)

echo.
echo [NOTICE] Neither Python nor Node.js was found in your system PATH.
echo You can still run the frontend easily:
echo   Method A: In IntelliJ IDEA, open project, right-click 'index.html' -> 'Open In' -> 'Browser'
echo   Method B: Install Python (https://www.python.org/downloads/) with 'Add Python to PATH' checked
echo   Method C: Use VS Code 'Live Server' extension
echo.
echo Opening index.html directly in browser...
start index.html

:end
pause
