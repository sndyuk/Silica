@echo off
if "%4"=="true" DEBUG_OPTIONS="-J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n -J-Djava.rmi.server.logCalls=true"
start /b rmiregistry %1 -J-Djava.rmi.server.hostname=%3 %DEBUG_OPTIONS -J-cp -J%2
