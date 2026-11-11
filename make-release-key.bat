@echo off
setlocal
cd /d %~dp0

REM ============================================================
REM  Hamgam Calendar - one-time release key maker
REM  Run this file ONCE on the laptop. It creates:
REM    1) my-upload-key.jks            (the private key - KEEP SAFE)
REM    2) KEYSTORE_BASE64_FOR_GITHUB.txt (text to paste in GitHub)
REM  Your password is typed only here and is never shown or sent.
REM ============================================================

if exist my-upload-key.jks (
  echo.
  echo STOP: my-upload-key.jks already exists in this folder.
  echo Nothing was changed. Tell Muse before doing anything else.
  pause
  exit /b 1
)

set "KEYTOOL="
if exist "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" set "KEYTOOL=C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"
if not defined KEYTOOL if defined JAVA_HOME if exist "%JAVA_HOME%\bin\keytool.exe" set "KEYTOOL=%JAVA_HOME%\bin\keytool.exe"
if not defined KEYTOOL set "KEYTOOL=keytool"

echo.
echo In a moment you will be asked to type a NEW password for the key.
echo The typing is hidden - nothing appears on screen while you type.
echo Type the password, press Enter, then type it again and press Enter.
echo After that, when it asks for a KEY password, just press Enter.
echo WRITE THE PASSWORD DOWN somewhere offline. If it is lost,
echo future updates of the app become impossible.
echo.
pause

"%KEYTOOL%" -genkeypair -v -keystore my-upload-key.jks -alias upload -keyalg RSA -keysize 2048 -validity 9125 -dname "CN=Hamgam Calendar, O=Sadra, C=IR"
if errorlevel 1 (
  echo.
  echo FAILED: the key was not created. Take a photo and tell Muse.
  pause
  exit /b 1
)

powershell -NoProfile -Command "[Convert]::ToBase64String([IO.File]::ReadAllBytes('my-upload-key.jks')) | Set-Content -NoNewline 'KEYSTORE_BASE64_FOR_GITHUB.txt'"
if errorlevel 1 (
  echo.
  echo The key was created, but the text file for GitHub failed.
  echo Tell Muse - do NOT run this file again.
  pause
  exit /b 1
)

echo.
echo DONE. Two files are now in this folder:
echo   my-upload-key.jks
echo   KEYSTORE_BASE64_FOR_GITHUB.txt
echo Keep both safe. Now tell Muse: the key is made.
pause
