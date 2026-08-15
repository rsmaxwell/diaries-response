@echo off
setlocal

rem ============================================================================
rem run-responder.bat
rem
rem Run the Diaries responder directly from the locally built class files and
rem runtime dependencies.
rem
rem Before starting the responder, regenerate its build information so that the
rem version and build details reported by the running responder reflect the
rem current source tree.
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
rem Define the responder directories used when constructing the Java classpath.
rem
rem GENERATED_BUILD_INFO_DIR contains the freshly generated build information.
rem It is deliberately placed before bin\main on the classpath so that an older
rem generated resource under bin\main cannot take precedence.
rem ----------------------------------------------------------------------------

set "RESPONDER_DIR=%PROJECT_DIR%\diaries-responder"
set "GENERATED_BUILD_INFO_DIR=%RESPONDER_DIR%\build\generated\resources\buildInfo"
set "RESPONDER_CLASSES_DIR=%RESPONDER_DIR%\bin\main"
set "RESPONDER_RUNTIME_DIR=%RESPONDER_DIR%\runtime"


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

if not exist "%PROJECT_DIR%\gradlew.bat" (
    echo ERROR: Gradle wrapper not found: "%PROJECT_DIR%\gradlew.bat" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)

if not exist "%CONFIG_FILE%" (
    echo ERROR: Responder configuration file not found: "%CONFIG_FILE%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Generate the responder build information.
rem
rem The responder exposes this information at runtime, so regenerate it
rem immediately before launching Java to ensure it represents the current
rem source tree.
rem ----------------------------------------------------------------------------

echo Generating responder build information...

call "%PROJECT_DIR%\gradlew.bat" :diaries-responder:generateBuildInfo
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo ERROR: Unable to generate responder build information. >&2
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Validate the locally built responder output.
rem
rem run-responder.bat runs the existing compiled classes rather than performing
rem a complete build. If the responder has not yet been compiled, report that
rem clearly rather than allowing Java to fail with a less useful class-loading
rem error.
rem ----------------------------------------------------------------------------

if not exist "%RESPONDER_CLASSES_DIR%" (
    echo ERROR: Responder classes directory not found: "%RESPONDER_CLASSES_DIR%" >&2
    echo Run the responder build before using this script. >&2
    set "EXIT_CODE=1"
    goto :cleanup
)

if not exist "%RESPONDER_RUNTIME_DIR%" (
    echo ERROR: Responder runtime directory not found: "%RESPONDER_RUNTIME_DIR%" >&2
    echo Run the responder dependency preparation before using this script. >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Construct the Java classpath.
rem
rem Put the freshly generated build-information resources first so that an
rem older copy under bin\main cannot take precedence.
rem ----------------------------------------------------------------------------

set "CLASSPATH=%GENERATED_BUILD_INFO_DIR%;%RESPONDER_CLASSES_DIR%;%RESPONDER_RUNTIME_DIR%\*"


rem ----------------------------------------------------------------------------
rem Start the responder.
rem
rem Capture Java's exit code immediately so that it can be returned unchanged
rem to the calling script or command prompt.
rem ----------------------------------------------------------------------------

echo Starting Diaries responder...
echo Configuration: "%CONFIG_FILE%"
echo.

java ^
    -classpath "%CLASSPATH%" ^
    com.rsmaxwell.diaries.responder.Responder ^
    --config "%CONFIG_FILE%"

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