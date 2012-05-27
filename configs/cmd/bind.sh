#!/bin/bash
pushd $(dirname $0) > /dev/null
################
#For degug.
if [ "$7" = "true" ]; then
  DEBUG_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
  echo "on debug mode. Waiting to connect a debugger listening on port: 8000"
fi
################
if [ "$2" != "" ]; then
    JAVA=$2/bin/java
else
    JAVA=java
fi

$JAVA -cp $3 \
     -Djava.rmi.server.hostname=$4 \
     $DEBUG_OPTIONS \
     com.silica.Silica \
     -c $5 \
     -o bind $6 > silica_bind.log 2> silica_bind.log < /dev/null &
     
echo "bind:"$5
echo "silica config path:" $6
popd > /dev/null
