#!/bin/bash
pushd $(dirname $0) > /dev/null
################
#For degug.
#if test $6 = "true" ; then
#  DEBUG_OPTIONS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
#  echo "on debug mode."
#fi
################
java -cp $2 -Djava.rmi.server.hostname=$3 $DEBUG_OPTIONS com.silica.Silica -c $4 -o bind $5 > silica_bind.log 2> silica_bind.log < /dev/null &
echo "bind:"$4
echo "silica config path:" $5
popd > /dev/null