@echo off
for %%f in ("%cd%") do set "copy_dir=%%~nxf"
del "%copy_dir%.zip" /s /q
robocopy . "%copy_dir%" /e /job:zip /xd "%copy_dir%"
"C:\Program Files\7-Zip\7z.exe" a -tzip "%copy_dir%" "%copy_dir%\*" -r
rmdir "%copy_dir%" /s /q