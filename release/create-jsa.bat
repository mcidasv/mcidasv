@echo off

if not exist "%1\jre\bin\javaw.exe" goto 10

"%1\jre\bin\java.exe" -Xshare:dump

:10