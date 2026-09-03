@echo off
setlocal

rem ============================================================================
rem clean.bat
rem
rem Clean the Diaries responder Gradle build output.
rem ============================================================================


rem ----------------------------------------------------------------------------
rem Initialise common script variables.
rem ----------------------------------------------------------------------------

set "SCRIPT_DIR=%~dp0"
set "EXIT_CODE=0"


rem ----------------------------------------------------------------------------
rem Locate the top-level Diaries project directory.
rem ----------------------------------------------------------------------------

pushd "%SCRIPT_DIR%..\..\.." >nul 2>&1
if errorlevel 1 (
    echo ERROR: Could not locate the Diaries project root. >&2
    echo Script directory: "%SCRIPT_DIR%" >&2
    endlocal & exit /b 1
)

set "PROJECT_DIR=%CD%"
set "RESPONDER_DIR=%PROJECT_DIR%\diaries-responder"
set "GRADLE_WRAPPER=%PROJECT_DIR%\gradlew.bat"


rem ----------------------------------------------------------------------------
rem Validate the paths required by this script.
rem ----------------------------------------------------------------------------

if not exist "%RESPONDER_DIR%" (
    echo ERROR: Responder directory not found: "%RESPONDER_DIR%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)

if not exist "%GRADLE_WRAPPER%" (
    echo ERROR: Gradle wrapper not found: "%GRADLE_WRAPPER%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Run the responder Gradle clean task.
rem ----------------------------------------------------------------------------

echo Cleaning Diaries responder...

call "%GRADLE_WRAPPER%" :diaries-responder:clean
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo ERROR: Diaries responder Gradle clean failed with exit code %EXIT_CODE%. >&2
    goto :cleanup
)

echo Diaries responder clean completed successfully.


rem ----------------------------------------------------------------------------
rem Common cleanup and exit.
rem ----------------------------------------------------------------------------

:cleanup
popd
endlocal & exit /b %EXIT_CODE%
