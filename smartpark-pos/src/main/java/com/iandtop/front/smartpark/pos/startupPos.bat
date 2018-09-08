set curdir=%~dp0
cd /d %curdir%

set path=%curdir%/Java1.8.101_32/bin
set classpath=%curdir%/Java1.8.101_32/jre/lib
java -Xms256m -Xmx256m  -jar smartpark-pos.jar

pause