#!/bin/bash
pushd $(dirname $0) > /dev/null
################
# For degub
#if test $4 = "true" ; then
#  DEBUG_OPTIONS="-J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,address=8001,suspend=n -J-Djava.rmi.server.logCalls=true"
#  echo "on debug mode."
#fi
################
rmiregistry $1 -J-Djava.rmi.server.hostname=$3 $DEBUG_OPTIONS -J-cp -J$2 > silica_rmi.log 2> silica_rmi.log < /dev/null &
echo "rmiregistry started."
popd > /dev/null

psid=-1
for psid in `ps x | grep "[r]miregistry $1"`; do echo "pid=" $psid;break; done
if test $psid -gt 0 ; then
  exit 0
fi
exit -1