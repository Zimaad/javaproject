@echo off
title SmartInterview Platform
echo ========================================================
echo   Starting SmartInterview Desktop Platform...
echo ========================================================
echo.
call "%USERPROFILE%\.m2\apache-maven-3.9.9\bin\mvn.cmd" javafx:run
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with an error. Press any key to close.
    pause > nul
)
