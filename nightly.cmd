@echo off
rem One command for the night. Examples:
rem   nightly check            (preflight + a tiny end-to-end run; do it before going to bed)
rem   nightly run  night1      (start or RESUME the run "night1")
rem   nightly status night1
rem   nightly stop night1      (graceful; the result is packed automatically)
rem   nightly pack night1
rem The config is ml\nightly_config.json (copy ml\nightly_config.example.json and edit the paths).
setlocal
cd /d "%~dp0"
set CMD=%1
set NAME=%2
if "%CMD%"=="" set CMD=check
if "%NAME%"=="" set NAME=night
if "%CMD%"=="check" (
  python ml\nightly.py check --config ml\nightly_config.json --name %NAME%
) else (
  python ml\nightly.py %CMD% --config ml\nightly_config.json --name %NAME%
)
exit /b %ERRORLEVEL%
