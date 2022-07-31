#!/bin/bash


# Change this to your netid
netid=mea130130

#
# Root directory of your project
PROJDIR=$HOME/aos/aos_project2/src

#
# This assumes your config file is named "config.txt"
# and is located in your project directory
#
CONFIG=$PROJDIR/config.txt

#
# Directory your java classes are in
#
BINDIR=$PROJDIR

#
# Your main project class
#
PROG=AOSProject2

x=0

head -n 24 $CONFIG | tail -n $((24-15+1)) | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    while read line 
    do
        host=$( echo $line | awk '{ print $2 }' )
		
		#echo $netid@$host.utdallas.edu java $BINDIR/$PROG $x &		
        #ssh $netid@$host.utdallas.edu java $BINDIR/$PROG $x &
		
		
		echo ssh -l $netid $host.utdallas.edu "killall java" &
		ssh -l $netid $host.utdallas.edu "killall java" &
		
        x=$(( x + 1 ))
    done
   
)


