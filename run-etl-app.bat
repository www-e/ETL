@echo off
echo Starting ETL Pipeline Application...
echo.

REM Start the Spring Boot application with Maven
start cmd /k "mvnw spring-boot:run"

REM Wait for the application to start
echo Waiting for the application to start...
timeout /t 10 /nobreak > nul

REM Open the default browser to the application URL
echo Opening browser...
start http://localhost:8080

echo.
echo ETL Pipeline Application started!
echo.
echo Press any key to exit this window (the application will continue running)
pause > nul
