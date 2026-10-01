@echo off
setlocal
cd /d "%~dp0.."
call mvn -f pom.xml install -N -B
if errorlevel 1 exit /b 1
call mvn -f backend/libs/fixna-platform-common/pom.xml clean install -DskipTests -B
if errorlevel 1 exit /b 1
echo Maven bootstrap complete (parent POM + fixna-platform-common:1.0.0).
