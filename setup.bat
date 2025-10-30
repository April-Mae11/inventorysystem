@echo off
REM Launcher for NVA Printing Services Inventory Management
echo ============================================
echo NVA Printing Services Inventory Management
echo ============================================

REM Ensure we run from the correct project directory
cd /d "%~dp0"
cd ..\inventorysystem

REM Optional: absolute path to project directory to be safe
set PROJECT_DIR=C:\Users\april\OneDrive\Desktop\inventorysystem\inventorysystem\inventorysystem
cd /d "%PROJECT_DIR%"

echo Running from: %CD%

REM Check Java
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install a JDK (Java 21 recommended) and set JAVA_HOME
    pause
    exit /b 1
)

REM Check Maven
mvn --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Apache Maven
    pause
    exit /b 1
)

echo Java and Maven detected.

echo Starting application via javafx-maven-plugin...
call mvn org.openjfx:javafx-maven-plugin:0.0.8:run

if %errorlevel% neq 0 (
    echo.
    echo ERROR: mvn javafx:run failed. Attempting fallback: run shaded jar if present
    if exist "target\inventory-management-1.0.0-shaded.jar" (
    echo Running shaded JAR...
    java -jar "target\inventory-management-1.0.0-shaded.jar"
    ) else (
        echo No shaded jar found. Please run the following command manually:
        echo mvn -f "%PROJECT_DIR%\pom.xml" org.openjfx:javafx-maven-plugin:0.0.10:run
    )
)

echo Application finished.
pause
