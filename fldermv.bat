@echo off
setlocal enabledelayedexpansion
for /r %%i in (.) do (
    if /i "%%~nxi"=="lawnchair" (
        set "folder=%%i"
        set "parent=%%~dpi"
        set "newname=yitap"
        echo Renaming !folder! to !parent!!newname!
        ren "!folder!" "!newname!"
    )
)
echo ok
pause