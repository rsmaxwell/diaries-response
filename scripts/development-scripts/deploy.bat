@echo on

set local 
pushd

set BASEDIR=%~dp0
set SCRIPT_DIR=%BASEDIR%\..
set SUBPROJECT_DIR=%SCRIPT_DIR%\..
set PROJECT_DIR=%SUBPROJECT_DIR%\..
set BUILD_DIR=%SUBPROJECT_DIR%\build

pushd %SCRIPT_DIR%
set SCRIPT_DIR=%CD%
popd

pushd %PROJECT_DIR%
set PROJECT_DIR=%CD%
popd

pushd %SUBPROJECT_DIR%
set SUBPROJECT_DIR=%CD%
popd



call %BUILD_DIR%\buildinfo.bat

cd %SUBPROJECT_DIR%

call %PROJECT_DIR%/gradlew publish --no-daemon --info --warning-mode all


popd
