@echo off
for %%f in ("%CD%") do set "copy_dir=%%~nxf"
robocopy . "%copy_dir%" /E /JOB:zip /xd "%copy_dir%"
"C:\Program Files\7-Zip\7z.exe" a -tzip "%copy_dir%" "%copy_dir%\*" -r
rmdir "%copy_dir%" /S /Q