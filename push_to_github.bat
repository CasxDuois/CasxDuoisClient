@echo off
chcp 65001 >nul
title CasxDuois - Push to GitHub
echo ============================================================
echo   CasxDuois Client - Đang đẩy code lên GitHub Repository
echo   Repo: https://github.com/CasxDuois/CasxDuoisClient
echo ============================================================
echo.
git push -u origin master --force
if %errorlevel% equ 0 (
    echo.
    echo ============================================================
    echo [THÀNH CÔNG] Đã đẩy toàn bộ mã nguồn lên GitHub thành công!
    echo ============================================================
) else (
    echo.
    echo [LƯU Ý] Nếu xuất hiện cửa sổ đăng nhập GitHub trên trình duyệt,
    echo hãy bấm xác nhận cấp quyền để hoàn tất quá trình push!
)
echo.
pause
