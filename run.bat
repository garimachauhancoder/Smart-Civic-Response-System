@echo off
echo ========================================================
echo   Smart Civic Response System - Pure Java 22 Launcher
echo ========================================================
if not exist "bin" mkdir bin
echo [1/2] Compiling Java backend...
javac -d bin -sourcepath src src\com\civic\Main.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java compilation failed!
    pause
    exit /b %ERRORLEVEL%
)
echo [2/2] Launching server on port 8080...
echo Portal URL: http://localhost:8080
java -cp bin com.civic.Main %*
