Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   Smart Civic Response System - Pure Java 22 Launcher" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "[1/2] Compiling Java backend..." -ForegroundColor Yellow
javac -d bin -sourcepath src src/com/civic/Main.java

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Java compilation failed!" -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "[2/2] Launching server on http://localhost:8080..." -ForegroundColor Green
java -cp bin com.civic.Main $args
