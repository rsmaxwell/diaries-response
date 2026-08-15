@echo off
setlocal

rem ============================================================================
rem PopulateDatabase.bat
rem
rem Run the legacy Diaries responder database-population utility using the
rem locally compiled responder classes and copied runtime dependencies.
rem
rem IMPORTANT:
rem The current source tree does not contain the legacy PopulateDatabase Java
rem class referenced by the original script. This script therefore validates
rem that the compiled class exists and reports a clear error when it does not.
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
set "RESPONDER_CLASSES_DIR=%RESPONDER_DIR%\bin\main"
set "RESPONDER_RUNTIME_DIR=%RESPONDER_DIR%\runtime"
set "RESPONDER_RESOURCES_DIR=%RESPONDER_DIR%\src\main\resources\META-INF"
set "CONFIG_FILE=%USERPROFILE%\.diaries\responder.json"

rem Retain the main class used by the historical script. If this utility is
rem reintroduced under a different package, update this value accordingly.
set "MAIN_CLASS=com.rsmaxwell.diaries.response.PopulateDatabase"
set "MAIN_CLASS_FILE=%RESPONDER_CLASSES_DIR%\com\rsmaxwell\diaries\response\PopulateDatabase.class"


rem ----------------------------------------------------------------------------
rem Validate the responder output, configuration, and legacy utility class.
rem ----------------------------------------------------------------------------

if not exist "%RESPONDER_DIR%" (
    echo ERROR: Responder directory not found: "%RESPONDER_DIR%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)

if not exist "%RESPONDER_CLASSES_DIR%" (
    echo ERROR: Responder classes directory not found: "%RESPONDER_CLASSES_DIR%" >&2
    echo Run build.bat before using this script. >&2
    set "EXIT_CODE=1"
    goto :cleanup
)

if not exist "%RESPONDER_RUNTIME_DIR%" (
    echo ERROR: Responder runtime directory not found: "%RESPONDER_RUNTIME_DIR%" >&2
    echo Run getDeps.bat before using this script. >&2
    set "EXIT_CODE=1"
    goto :cleanup
)

if not exist "%CONFIG_FILE%" (
    echo ERROR: Responder configuration file not found: "%CONFIG_FILE%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)

if not exist "%MAIN_CLASS_FILE%" (
    echo ERROR: Legacy database-population class not found: >&2
    echo "%MAIN_CLASS_FILE%" >&2
    echo. >&2
    echo The current Diaries source tree does not contain %MAIN_CLASS%. >&2
    echo This script cannot populate the database until that utility is restored >&2
    echo or replaced with the current database-initialisation mechanism. >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Construct the Java classpath.
rem
rem Wildcard expansion in the runtime directory allows Java to load all copied
rem dependency JARs without manually enumerating them in the batch script.
rem ----------------------------------------------------------------------------

set "CLASSPATH=%RESPONDER_CLASSES_DIR%;%RESPONDER_RESOURCES_DIR%;%RESPONDER_RUNTIME_DIR%\*"
set "LOGGER_LEVEL=DEBUG"


rem ----------------------------------------------------------------------------
rem Run the legacy population utility and return its result to the caller.
rem ----------------------------------------------------------------------------

echo Running Diaries database population utility...

java ^
    -classpath "%CLASSPATH%" ^
    %MAIN_CLASS% ^
    --config "%CONFIG_FILE%"

set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo ERROR: Database population utility exited with code %EXIT_CODE%. >&2
    goto :cleanup
)

echo Database population completed successfully.


rem ----------------------------------------------------------------------------
rem Common cleanup and exit.
rem ----------------------------------------------------------------------------

:cleanup
popd
endlocal & exit /b %EXIT_CODE%
