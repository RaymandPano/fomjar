@echo off

::set opt=-J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8844

cd ..
java -cp lib\fomjar-server-0.0.1.jar fomjar.server.FjServerLauncher %opt% %1 %2 %3 %4 %5 %6 %7 %8 %9
cd bin
pause
