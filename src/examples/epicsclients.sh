#!/bin/sh
CAMONITOR=`which camonitor 2>/dev/null`
if [ $CAMONITOR ]; then 
    $CAMONITOR gpi:health0 gpi:health1 gpi:health2 gpi:health3 gpi:health4 gpi:health5 gpi:health6 gpi:health7 gpi:health8 gpi:health9 gpi:status0  gpi:status1  gpi:status2  gpi:status3  gpi:status4  gpi:status5  gpi:status6  gpi:status7  gpi:status8  gpi:status9 gpi:alarm0  gpi:alarm1  gpi:alarm2  gpi:alarm3  gpi:alarm4  gpi:alarm5  gpi:alarm6  gpi:alarm7  gpi:alarm8  gpi:alarm9  
else
    echo "Please install the epics-base RPM package";
fi
