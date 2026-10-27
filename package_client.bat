@echo off
setlocal enabledelayedexpansion
title CasxDuois Client - Packaging 1-Click Standalone
echo ============================================================
echo   CasxDuois Client - Dong goi ban 1-Click Standalone (.exe)
echo ============================================================
echo.

echo [1/4] Bien dich Mod Client JAR moi nhat...
call gradlew.bat build -x test
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Loi bien dich Gradle!
    pause
    exit /b %ERRORLEVEL%
)

echo [2/4] Bien dich CasxDuoisLauncher.exe...
"C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /target:winexe /out:CasxDuoisLauncher.exe /lib:"C:\Windows\Microsoft.NET\Framework64\v4.0.30319\WPF" /r:PresentationFramework.dll /r:PresentationCore.dll /r:WindowsBase.dll /r:System.Xaml.dll /r:System.dll /r:System.Core.dll /r:System.IO.Compression.dll /r:System.IO.Compression.FileSystem.dll /win32icon:app.ico "src\launcher_native\CasxDuoisLauncherApp.cs"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Loi bien dich CasxDuoisLauncher.exe!
    pause
    exit /b %ERRORLEVEL%
)

echo [3/4] Tao goi du lieu nen client_payload.zip...
python "scratch_package.py"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Loi tao payload zip!
    pause
    exit /b %ERRORLEVEL%
)

echo [4/4] Bien dich CasxDuoisClient.exe tich hop san du lieu...
"C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe" /target:winexe /out:CasxDuoisClient.exe /lib:"C:\Windows\Microsoft.NET\Framework64\v4.0.30319\WPF" /r:PresentationFramework.dll /r:PresentationCore.dll /r:WindowsBase.dll /r:System.Xaml.dll /r:System.dll /r:System.Core.dll /r:System.IO.Compression.dll /r:System.IO.Compression.FileSystem.dll /r:Microsoft.CSharp.dll /win32icon:app.ico /resource:client_payload.zip,ClientPayload "src\launcher_native\CasxDuoisOneClick.cs"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Loi bien dich csc.exe!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ============================================================
echo   HOAN TAT! File dau ra duy nhat: CasxDuoisClient.exe
echo   (Chi can copy file nay sang bat ky may nao la choi duoc ngay)
echo ============================================================
pause
