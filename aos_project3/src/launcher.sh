#!/bin/bash
protocolx=${1-tcp}
[ $# -eq 0 ] && { echo "Usage: $0 protocol" ; exit 1; }
echo "Running AOSProject3 using Protocol:$protocolx"

#Clean output directory
rm -rf output/*.txt

#Clean log file
rm -rf log.txt
rm -rf log/*.*
rm -rf csviolation/*.*
rm -rf exception/*.*

# Change this to your netid
netid=mea130130

#
# Root directory of your project
PROJDIR=$HOME/aos/aos_project3/src

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
PROG=AOSProject3

x=0

head -n 24 $CONFIG | tail -n $((24-15+1)) | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    while read line 
    do
        host=$( echo $line | awk '{ print $2 }' )
		
		#echo $netid@$host.utdallas.edu java $BINDIR/$PROG $x $protocolx &		
        #ssh $netid@$host.utdallas.edu java $BINDIR/$PROG $x $protocolx &
		
		
		#echo ssh -l $netid $host.utdallas.edu "cd $BINDIR;java $PROG $x $protocolx" &
		ssh -l $netid $host.utdallas.edu "cd $BINDIR;java $PROG $x $protocolx" &
		
        x=$(( x + 1 ))
    done
   
)


