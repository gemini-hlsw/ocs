#!/bin/bash
# This is a script use to run the benchmark tests from GIAPI. 
# The script receives as an argument the LD_LIBRARY_PATH that needs to
# be used. 
#
# This script is called from the Makefile in the test area code. 
# It should not be called directly from the command line.e `uname` in
case `uname` in
    Darwin*) export DYLD_LIBRARY_PATH=$1 ;;
          *) export LD_LIBRARY_PATH=$1 ;;
esac

./libgiapi-integration
