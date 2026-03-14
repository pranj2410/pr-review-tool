@ECHO OFF
SETLOCAL

SET APP_HOME=%~dp0
SET GRADLE_VERSION=8.5
SET DIST_NAME=gradle-%GRADLE_VERSION%-bin
SET DIST_URL=https://services.gradle.org/distributions/%DIST_NAME%.zip
SET DIST_DIR=%APP_HOME%\.gradle-dist
SET INSTALL_DIR=%DIST_DIR%\gradle-%GRADLE_VERSION%
SET ZIP_PATH=%DIST_DIR%\%DIST_NAME%.zip

where gradle >NUL 2>NUL
IF %ERRORLEVEL% EQU 0 (
    gradle %*
    EXIT /B %ERRORLEVEL%
)

IF NOT EXIST "%DIST_DIR%" mkdir "%DIST_DIR%"

IF NOT EXIST "%INSTALL_DIR%\bin\gradle.bat" (
    ECHO Gradle %GRADLE_VERSION% not found locally. Downloading distribution...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ZIP_PATH%'; Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%DIST_DIR%' -Force"
)

CALL "%INSTALL_DIR%\bin\gradle.bat" %*
EXIT /B %ERRORLEVEL%
