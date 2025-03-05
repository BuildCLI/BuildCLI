@echo off
setlocal
echo Validating current directory...
for %%i in ("%cd%") do (
    if /i "%%~nxi"=="BuildCLI" (
        echo Current directory is 'BuildCLI'. Checking for Git repository...
        if exist ".git" (
            echo '.git' directory found. Skipping repository setup.
        ) else (
            echo '.git' directory not found. Ensure this is the correct repository.
            pause
            exit /b 1
        )
    ) else (
        echo Current directory is NOT 'BuildCLI'. Checking if BuildCLI exists...
        if not exist "BuildCLI" (
            echo 'BuildCLI' directory not found. Cloning repository...
            git clone https://github.com/BuildCLI/BuildCLI.git || (
                echo Failed to clone repository. Make sure Git is installed.
                pause
                exit /b 1
            )
        )
        cd BuildCLI || (
            echo Failed to access BuildCLI directory.
            pause
            exit /b 1
        )
    )
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
copy /Y cli\target\buildcli.jar "%USERPROFILE%\bin\buildcli.jar" >nul
echo Creating buildcli.bat shortcut...
(
    echo @echo off
    echo java --enable-preview --add-modules jdk.incubator.vector -jar "%%USERPROFILE%%\bin\buildcli.jar"
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
