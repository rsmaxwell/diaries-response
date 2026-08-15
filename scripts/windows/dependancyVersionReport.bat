@echo off
setlocal

rem ============================================================================
rem dependancyVersionReport.bat
rem
rem Generate the Gradle dependency-version report for the Diaries responder.
rem
rem NOTE: The historical filename contains "dependancy". It is retained here to
rem avoid breaking existing callers, even though "dependency" is the correct
rem spelling.
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
set "GRADLE_WRAPPER=%PROJECT_DIR%\gradlew.bat"


rem ----------------------------------------------------------------------------
rem Validate the Gradle wrapper before generating the report.
rem ----------------------------------------------------------------------------

if not exist "%GRADLE_WRAPPER%" (
    echo ERROR: Gradle wrapper not found: "%GRADLE_WRAPPER%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Generate the responder dependency update report.
rem
rem --no-parallel is retained from the existing script so report generation is
rem deterministic and avoids unnecessary concurrent Gradle work.
rem ----------------------------------------------------------------------------

echo Generating Diaries responder dependency version report...

call "%GRADLE_WRAPPER%" :diaries-responder:dependencyUpdates --no-parallel
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo ERROR: Dependency version report failed with exit code %EXIT_CODE%. >&2
    goto :cleanup
)

echo Dependency version report completed successfully.


rem ----------------------------------------------------------------------------
rem Common cleanup and exit.
rem ----------------------------------------------------------------------------

:cleanup
popd
endlocal & exit /b %EXIT_CODE%
