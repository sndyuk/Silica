for /f "tokens=2 delims=," %%i in ('tasklist /v /fo csv /nh /fi "imagename eq rmiregistry.exe"') do taskkill /F /pid %%~i
ping localhost -n 1 > nul
