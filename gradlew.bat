@ECHO OFF
setlocal

set JAVA_EXE=java.exe
if defined JAVA_HOME set JAVA_EXE=%JAVA_HOME%\bin\java.exe

if not exist "%JAVA_EXE%" (
  echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
  exit /b 1
)

set WRAPPER_JAR=%~dp0\gradle\wrapper\gradle-wrapper.jar

"%JAVA_EXE%" -jar "%WRAPPER_JAR%" %*
