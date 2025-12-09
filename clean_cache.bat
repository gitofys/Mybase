@echo off
echo Cleaning Gradle cache...
echo.

REM Stop Gradle daemon
call gradlew --stop

REM Clean project
call gradlew clean

REM Delete Gradle cache for version 7.5 (scripts cache causing Java 21 issue)
echo.
echo Deleting Gradle 7.5 scripts cache...
if exist "%USERPROFILE%\.gradle\caches\7.5\scripts" (
    rmdir /s /q "%USERPROFILE%\.gradle\caches\7.5\scripts"
    echo Gradle 7.5 scripts cache deleted.
) else (
    echo Gradle 7.5 scripts cache not found.
)

REM Delete Gradle cache for version 8.13
echo.
echo Deleting Gradle 8.13 cache...
if exist "%USERPROFILE%\.gradle\caches\8.13" (
    rmdir /s /q "%USERPROFILE%\.gradle\caches\8.13"
    echo Gradle 8.13 cache deleted.
) else (
    echo Gradle 8.13 cache not found.
)

REM Delete transforms cache
if exist "%USERPROFILE%\.gradle\caches\transforms" (
    rmdir /s /q "%USERPROFILE%\.gradle\caches\transforms"
    echo Transforms cache deleted.
)

REM Delete daemon cache
if exist "%USERPROFILE%\.gradle\daemon" (
    rmdir /s /q "%USERPROFILE%\.gradle\daemon"
    echo Gradle daemon cache deleted.
)

echo.
echo Cache cleanup complete!
echo Please rebuild your project now.
pause

