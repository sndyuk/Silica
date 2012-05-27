@echo off
if "%5"=="true" DEBUG_OPTIONS="-J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n -J-Djava.rmi.server.logCalls=true"
start /b rmiregistry %1 -J-Djava.rmi.server.hostname=%4 %DEBUG_OPTIONS -J-cp -J%3
