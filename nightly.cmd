@echo off
rem One command for the night pipeline (Windows).
rem
rem   nightly check              preflight + a tiny end-to-end run (do this before going to bed)
rem   nightly run night1         start or RESUME the run "night1"        (extra: --hours 12)
rem   nightly status night1      what is going on
rem   nightly stop night1        graceful stop, the result is packed automatically
rem   nightly pack night1        pack the result by hand
rem
rem Config: ml\nightly_config.json (override with the environment variable ARES_CONFIG).
rem Python: found automatically (py -3, then python); override with ARES_PYTHON, e.g. set ARES_PYTHON=C:\Python312\python.exe
setlocal
cd /d "%~dp0"

set "CMD=%~1"
set "NAME=%~2"
if "%CMD%"=="" set "CMD=check"
if "%NAME%"=="" set "NAME=night"
if "%ARES_CONFIG%"=="" set "ARES_CONFIG=ml\nightly_config.json"

rem ---- find a REAL Python 3 (the python.exe in WindowsApps is only a stub that opens the Microsoft Store)
set "PY="
if defined ARES_PYTHON (
  "%ARES_PYTHON%" -c "import sys; sys.exit(0 if sys.version_info[0]==3 else 1)" >nul 2>&1 && set "PY="%ARES_PYTHON%""
)
if not defined PY (
  py -3 -c "import sys; sys.exit(0 if sys.version_info[0]==3 else 1)" >nul 2>&1 && set "PY=py -3"
)
if not defined PY (
  python -c "import sys; sys.exit(0 if sys.version_info[0]==3 else 1)" >nul 2>&1 && set "PY=python"
)
if not defined PY (
  echo.
  echo [nightly] No working Python 3 was found. The "python" command is probably only the Microsoft Store stub.
  echo [nightly] Install Python 3.10 or newer:   winget install Python.Python.3.12
  echo [nightly]   or from https://www.python.org/downloads/  ^(tick "Add python.exe to PATH"^)
  echo [nightly] then open a NEW terminal and run:   py -3 -m pip install numpy
  echo [nightly] If Python is installed in an unusual place:   set ARES_PYTHON=C:\full\path\to\python.exe
  exit /b 2
)
echo [nightly] python: %PY%   config: %ARES_CONFIG%   command: %CMD% %NAME%

%PY% ml\nightly.py %CMD% --config "%ARES_CONFIG%" --name %NAME% %3 %4 %5 %6 %7 %8
exit /b %ERRORLEVEL%
