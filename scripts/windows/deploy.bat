@echo off
setlocal

rem ============================================================================
rem deploy.bat
rem
rem Publish the Diaries responder Maven artifact using the Gradle publishing
rem configuration defined by diaries-responder\build.gradle.
rem
rem The current Gradle build calculates the responder version itself and reads
rem publishing credentials from Gradle properties, so the legacy buildinfo.bat
rem step is no longer required here.
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
rem Validate the Gradle wrapper before attempting publication.
rem ----------------------------------------------------------------------------

if not exist "%GRADLE_WRAPPER%" (
    echo ERROR: Gradle wrapper not found: "%GRADLE_WRAPPER%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Publish the responder artifact.
rem
rem The Gradle publishing configuration performs its own validation of the
rem required Maven credentials. Capture the Gradle exit code immediately and
rem return it to the caller if publication fails.
rem ----------------------------------------------------------------------------

echo Publishing Diaries responder artifact...

call "%GRADLE_WRAPPER%" :diaries-responder:publish --no-daemon --info --warning-mode all
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo ERROR: Diaries responder publication failed with exit code %EXIT_CODE%. >&2
    goto :cleanup
)

echo Diaries responder artifact published successfully.


rem ----------------------------------------------------------------------------
rem Common cleanup and exit.
rem ----------------------------------------------------------------------------

:cleanup
popd
endlocal & exit /b %EXIT_CODE%
