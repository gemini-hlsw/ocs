#!/bin/sh

if [ -z "$1" ]; 
then
	echo "usage: $0 <test-program>"
    exit
fi

if [ `uname | grep -c "Darwin"`  = 1 ];
then
    export DYLD_LIBRARY_PATH=`make run`
else
    export LD_LIBRARY_PATH=`make run`
fi
./$1

