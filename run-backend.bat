@echo off
title EstateX - Spring Boot Backend Runner
cd /d "%~dp0backend"
echo ========================================================
echo   Starting EstateX Spring Boot Backend (Port 8081)...
echo ========================================================

REM Check if JAVA_HOME is set, otherwise try to detect
if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
) else (
    REM Try common JDK 21 / 17 locations
    if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
        set "PATH=C:\Program Files\Java\jdk-21\bin;%PATH%"
    ) else if exist "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe" (
        for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do (
            set "JAVA_HOME=%%D"
            set "PATH=%%D\bin;%PATH%"
        )
    )
)

REM Verify Java is available
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java is not detected in your PATH or JAVA_HOME.
    echo Please install JDK 21 or JDK 17 and make sure it is in your system PATH.
    echo You can download it from: https://adoptium.net/
    pause
    exit /b 1
)

echo Java detected:
java -version
echo.

REM Prefer Maven Wrapper (no separate Maven install required)
if exist "mvnw.cmd" (
    echo Using Maven Wrapper (mvnw.cmd)...
    call mvnw.cmd spring-boot:run
) else (
    where mvn >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        echo Using system Maven...
        mvn spring-boot:run
    ) else (
        echo [ERROR] Maven not found.
        echo If using IntelliJ IDEA, simply open the project and run EstateXApplication.java.
        pause
        exit /b 1
    )
)

pause
