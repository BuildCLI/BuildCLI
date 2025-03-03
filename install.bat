@echo off
setlocal

if exist "BuildCLI\.git" (
    echo Project directory already exists. Pulling latest changes...
    cd BuildCLI
    git pull origin main || (
        echo Failed to update repository.
        pause
        exit /b 1
    )
    cd ..
) else (
    echo Cloning the BuildCLI repository...
    git clone https://github.com/BuildCLI/BuildCLI.git || (
        echo Failed to clone repository. Make sure Git is installed.
        pause
        exit /b 1
    )
)

cd BuildCLI || (
    echo Directory 'BuildCLI' not found. Cloning may have failed.
    pause
    exit /b 1
)

echo Checking if Maven is installed...
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo Maven not found. Please install Maven.
    pause
    exit /b 1
)

echo Checking if Java is installed...
where java >nul 2>nul
if %errorlevel% neq 0 (
    echo Java not found. Please install Java.
    pause
    exit /b 1
)

echo Building and packaging the project...
mvn clean package -DskipTests || (
    echo Maven build failed.
    pause
    exit /b 1
)

echo Configuring BuildCLI for global access...

if not exist "%USERPROFILE%\bin" (
    echo Creating directory %USERPROFILE%\bin...
    mkdir "%USERPROFILE%\bin"
)

echo Copying BuildCLI JAR to %USERPROFILE%\bin...
copy cli\target\buildcli.jar "%USERPROFILE%\bin\buildcli.jar" >nul

echo Creating buildcli.bat shortcut...
(
    echo @echo off
    echo java --enable-preview --add-modules jdk.incubator.vector -jar "%%USERPROFILE%%\bin\buildcli.jar" %%*
) > "%USERPROFILE%\bin\buildcli.bat"

echo Ensuring %USERPROFILE%\bin is in the PATH...
echo If the command fails, add this manually to your environment variables:
echo.
echo setx PATH "%%PATH%%;%USERPROFILE%\bin"
echo.

echo Installation completed!
echo You can now run BuildCLI using:
echo buildcli
pause
endlocal
