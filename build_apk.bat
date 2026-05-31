@echo off
chcp 65001 >nul
echo ========================================
echo   Image Sequence Player - Build APK
echo ========================================
echo.

set "SDK_PATH=%LOCALAPPDATA%\Android\Sdk"

if exist "%SDK_PATH%" (
    echo [OK] Android SDK found: %SDK_PATH%
    echo sdk.dir=%SDK_PATH:\=/% > local.properties
) else (
    echo [WARN] Android SDK not found at default path.
    echo Please install Android Studio first:
    echo https://developer.android.com/studio
    echo.
    echo Or enter your SDK path manually:
    set /p SDK_PATH="SDK Path: "
    if not "%SDK_PATH%"=="" (
        echo sdk.dir=%SDK_PATH:\=/% > local.properties
    )
)

echo.
echo Building Debug APK...
echo.

call gradlew.bat assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   BUILD SUCCESS!
    echo ========================================
    echo.
    echo APK: app\build\outputs\apk\debug\app-debug.apk
    echo.
    explorer app\build\outputs\apk\debug\
) else (
    echo.
    echo BUILD FAILED!
    echo Please open this project in Android Studio and build from there.
)

pause