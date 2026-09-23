@echo off
setlocal
set "MAVEN_CMD=%USERPROFILE%\.m2\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%MAVEN_CMD%" (
    "%MAVEN_CMD%" %*
) else (
    mvn %*
)
