start /b rmiregistry %1 -J-cp -J%2
rem start /b rmiregistry %1 -J-Djava.rmi.server.hostname=%3 -J-cp -J%1
ping localhost -n 4 > nul
