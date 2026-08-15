@echo off
setlocal

rem ============================================================================
rem build.bat
rem
rem Clean and build the Diaries responder using the Gradle wrapper from the
rem top-level Diaries project.
rem ============================================================================


rem ----------------------------------------------------------------------------
rem Initialise common script variables.
rem ----------------------------------------------------------------------------

set "SCRIPT_DIR=%~dp0"
set "EXIT_CODE=0"


rem ----------------------------------------------------------------------------
rem Locate the top-level Diaries project directory.
rem
rem This script is located under:
rem
rem     diaries-responder\scripts\windows
rem
rem Moving up three levels therefore gives us the Diaries project root.
rem ----------------------------------------------------------------------------

pushd "%SCRIPT_DIR%..\..\.." >nul 2>&1
if errorlevel 1 (
    echo ERROR: Could not locate the Diaries project root. >&2
    echo Script directory: "%SCRIPT_DIR%" >&2
    endlocal & exit /b 1
)

set "PROJECT_DIR=%CD%"
set "GRADLE_WRAPPER=%PROJECT_DIR%\gradlew.bat"


rem ----------------------------------------------------------------------------
rem Validate the Gradle wrapper before attempting the build.
rem ----------------------------------------------------------------------------

if not exist "%GRADLE_WRAPPER%" (
    echo ERROR: Gradle wrapper not found: "%GRADLE_WRAPPER%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Clean and build the responder.
rem
rem Capture Gradle's exit code immediately so it can be returned unchanged to
rem the caller.
rem ----------------------------------------------------------------------------

echo Building Diaries responder...

call "%GRADLE_WRAPPER%" :diaries-responder:clean :diaries-responder:build --info
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo ERROR: Diaries responder build failed with exit code %EXIT_CODE%. >&2
    goto :cleanup
)

echo Diaries responder build completed successfully.


rem ----------------------------------------------------------------------------
rem Common cleanup and exit.
rem ----------------------------------------------------------------------------

:cleanup
popd
endlocal & exit /b %EXIT_CODE%
