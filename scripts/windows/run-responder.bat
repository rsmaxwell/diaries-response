@echo off
setlocal

rem ============================================================================
rem run-responder.bat
rem
rem Build and run the Diaries responder using the Gradle Application
rem distribution.
rem
rem This follows the same launcher pattern as diaries-web: Gradle prepares the
rem distribution, then this script delegates to Gradle's generated Windows
rem launcher.
rem ============================================================================


rem ----------------------------------------------------------------------------
rem Initialise common script variables.
rem
rem SCRIPT_DIR is the directory containing this script.
rem EXIT_CODE is returned to the calling process when the script finishes.
rem ----------------------------------------------------------------------------

set "SCRIPT_DIR=%~dp0"
set "EXIT_CODE=0"


rem ----------------------------------------------------------------------------
rem Locate the Diaries project directory.
rem
rem This script is located under:
rem
rem     diaries-responder\scripts\windows
rem
rem Moving up three levels therefore gives us the top-level Diaries project
rem directory.
rem
rem Exit immediately if that directory cannot be located. Since pushd has not
rem succeeded in that case, there is no corresponding popd to perform.
rem ----------------------------------------------------------------------------

pushd "%SCRIPT_DIR%..\..\.." >nul 2>&1
if errorlevel 1 (
    echo ERROR: Could not locate the Diaries project root. >&2
    echo Script directory: "%SCRIPT_DIR%" >&2
    endlocal & exit /b 1
)

set "PROJECT_DIR=%CD%"


rem ----------------------------------------------------------------------------
rem Define the responder directory, Gradle wrapper and generated launcher.
rem ----------------------------------------------------------------------------

set "RESPONDER_DIR=%PROJECT_DIR%\diaries-responder"
set "GRADLE_WRAPPER=%PROJECT_DIR%\gradlew.bat"
set "LAUNCHER=%RESPONDER_DIR%\build\install\diaries-responder\bin\diaries-responder.bat"


rem ----------------------------------------------------------------------------
rem Define the responder configuration file.
rem
rem The development responder configuration is stored beneath the current
rem user's profile rather than within the source tree.
rem ----------------------------------------------------------------------------

set "CONFIG_FILE=%USERPROFILE%\.diaries\responder.json"


rem ----------------------------------------------------------------------------
rem Define the logging levels used for local responder development.
rem ----------------------------------------------------------------------------

set "HIBERNATE_LOGLEVEL=OFF"
set "LOGLEVEL=INFO"


rem ----------------------------------------------------------------------------
rem Validate files and directories required before starting the responder.
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

if not exist "%CONFIG_FILE%" (
    echo ERROR: Responder configuration file not found: "%CONFIG_FILE%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Prepare the responder runtime distribution. The installDist task compiles
rem the current source, regenerates build information and assembles the runtime
rem classpath used by Gradle's generated launcher.
rem ----------------------------------------------------------------------------

echo Preparing the Diaries responder runtime distribution...

call "%GRADLE_WRAPPER%" :diaries-responder:installDist
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo ERROR: Could not prepare the Diaries responder runtime distribution. >&2
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Verify that Gradle created the generated application launcher.
rem ----------------------------------------------------------------------------

if not exist "%LAUNCHER%" (
    echo ERROR: Diaries responder launcher was not created: "%LAUNCHER%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Start the responder.
rem
rem Capture Java's exit code immediately so that it can be returned unchanged
rem to the calling script or command prompt.
rem ----------------------------------------------------------------------------

echo Starting Diaries responder...
echo Configuration: "%CONFIG_FILE%"
echo.

call "%LAUNCHER%" --config "%CONFIG_FILE%"

set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo ERROR: Diaries responder exited with code %EXIT_CODE%. >&2
)


rem ----------------------------------------------------------------------------
rem Common cleanup and exit.
rem
rem The initial project-directory pushd succeeded before any path can reach this
rem label, so it is safe to restore the caller's original working directory.
rem ----------------------------------------------------------------------------

:cleanup
popd
endlocal & exit /b %EXIT_CODE%
