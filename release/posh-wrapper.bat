@echo OFF
set INSTALL_DIR=%1
set POSH_SCRIPT=%INSTALL_DIR%\local-adde.ps1
powershell -noprofile -nologo -executionpolicy bypass -File %POSH_SCRIPT% -mcvpath %INSTALL_DIR%