@echo off
setlocal

rem ============================================================================
rem getDeps.bat
rem
rem Copy the Diaries responder runtime dependencies into the responder runtime
rem directory using the Gradle getDeps task.
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
set "RUNTIME_DIR=%RESPONDER_DIR%\runtime"
set "GRADLE_WRAPPER=%PROJECT_DIR%\gradlew.bat"


rem ----------------------------------------------------------------------------
rem Validate the paths required by the dependency-copy operation.
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
rem Copy the responder runtime dependencies.
rem
rem The Gradle getDeps task writes the runtime classpath dependencies into the
rem diaries-responder\runtime directory.
rem ----------------------------------------------------------------------------

echo Copying Diaries responder runtime dependencies...

call "%GRADLE_WRAPPER%" :diaries-responder:getDeps
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo ERROR: Could not copy responder runtime dependencies; Gradle exited with code %EXIT_CODE%. >&2
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Verify that the expected runtime directory was created.
rem ----------------------------------------------------------------------------

if not exist "%RUNTIME_DIR%" (
    echo ERROR: Runtime dependency directory was not created: "%RUNTIME_DIR%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)

echo Responder runtime dependencies copied successfully.


rem ----------------------------------------------------------------------------
rem Common cleanup and exit.
rem ----------------------------------------------------------------------------

:cleanup
popd
endlocal & exit /b %EXIT_CODE%
