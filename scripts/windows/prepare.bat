@echo off
setlocal

rem ============================================================================
rem prepare.bat
rem
rem Generate the responder build-information batch file used by the development
rem scripts.
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
rem Locate the diaries-responder project directory.
rem
rem This script is located under:
rem
rem     diaries-responder\scripts\windows
rem
rem Moving up two levels therefore gives us the diaries-responder directory.
rem Exit immediately if this directory cannot be located. Since pushd has not
rem succeeded in that case, there is no corresponding popd to perform.
rem ----------------------------------------------------------------------------

pushd "%SCRIPT_DIR%..\.." >nul 2>&1
if errorlevel 1 (
    echo ERROR: Could not locate the diaries-responder project root. >&2
    echo Script directory: "%SCRIPT_DIR%" >&2
    endlocal & exit /b 1
)

set "SUBPROJECT_DIR=%CD%"


rem ----------------------------------------------------------------------------
rem Define the build directory and generated build-information file.
rem
rem Keeping these as explicit variables makes the paths easier to understand
rem and avoids repeatedly constructing them later in the script.
rem ----------------------------------------------------------------------------

set "BUILD_DIR=%SUBPROJECT_DIR%\build"
set "BUILD_INFO_FILE=%BUILD_DIR%\buildinfo.bat"


rem ----------------------------------------------------------------------------
rem Define the build information to be written to buildinfo.bat.
rem
rem VERSION is currently fixed here. If responder versioning is instead meant
rem to come from Gradle/Git, this would be a good candidate for replacing with
rem the authoritative Gradle-derived version in a later change.
rem ----------------------------------------------------------------------------

set "BUILD_ID=none"
set "VERSION=0.0.1-SNAPSHOT"
set "REPOSITORY=snapshots"


rem ----------------------------------------------------------------------------
rem Generate a locale-independent build timestamp.
rem
rem Do not derive this from the Windows DATE and TIME environment variables,
rem because their formatting depends on the machine's regional settings.
rem PowerShell gives us an explicit and predictable representation instead.
rem ----------------------------------------------------------------------------

set "TIMESTAMP="

for /f "usebackq delims=" %%I in (`
    powershell -NoProfile -Command "Get-Date -Format 'yyyy-MM-dd HH:mm:ss'"
`) do set "TIMESTAMP=%%I"

if not defined TIMESTAMP (
    echo ERROR: Could not generate build timestamp. >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Ensure that the build directory exists.
rem
rem The directory may not yet exist on a clean checkout, so create it when
rem necessary and report a useful error if that fails.
rem ----------------------------------------------------------------------------

if not exist "%BUILD_DIR%" (
    mkdir "%BUILD_DIR%" >nul 2>&1
    if errorlevel 1 (
        echo ERROR: Could not create build directory: "%BUILD_DIR%" >&2
        set "EXIT_CODE=1"
        goto :cleanup
    )
)


rem ----------------------------------------------------------------------------
rem Generate buildinfo.bat.
rem
rem Quoted SET syntax is used in the generated file so that accidental trailing
rem spaces cannot become part of the variable values.
rem ----------------------------------------------------------------------------

(
    echo set "BUILD_ID=%BUILD_ID%"
    echo set "VERSION=%VERSION%"
    echo set "REPOSITORY=%REPOSITORY%"
    echo set "TIMESTAMP=%TIMESTAMP%"
) > "%BUILD_INFO_FILE%"

if errorlevel 1 (
    echo ERROR: Could not create build information file: "%BUILD_INFO_FILE%" >&2
    set "EXIT_CODE=1"
    goto :cleanup
)


rem ----------------------------------------------------------------------------
rem Report successful generation of the build-information file.
rem ----------------------------------------------------------------------------

echo Build information written to:
echo "%BUILD_INFO_FILE%"


rem ----------------------------------------------------------------------------
rem Common cleanup and exit.
rem
rem The initial pushd succeeded before any path can reach this label, so it is
rem safe to restore the caller's original working directory with popd.
rem ----------------------------------------------------------------------------

:cleanup
popd
endlocal & exit /b %EXIT_CODE%
