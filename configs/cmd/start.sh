#!/bin/bash
pushd $(dirname $0) > /dev/null
################
echo " "
# For degub
if [ "$5" = "true" ]; then
  DEBUG_OPTIONS="-J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=y -J-Djava.rmi.server.logCalls=true"
  echo "on debug mode. Waiting to connect a debugger listening on port: 8000"
fi
################
if [ "$2" != "" ]; then
    RMIREG=$2/bin/rmiregistry
else
    RMIREG=rmiregistry
fi

$RMIREG $1 -J-Djava.rmi.server.hostname=$4 $DEBUG_OPTIONS -J-cp -J$3 > silica_rmi.log 2> silica_rmi.log < /dev/null &
echo "rmiregistry started."
popd > /dev/null

psid=-1
for psid in `ps x | grep "[r]miregistry $1"`; do echo "pid=" $psid;break; done
if test $psid -gt 0 ; then
  exit 0
fi
exit -1