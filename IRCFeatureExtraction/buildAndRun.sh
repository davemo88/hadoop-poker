#!/bin/bash

if [ "$#" -e 0 ]; 
then
    fsInputDir="IRCData"
else 
    fsInputDir="IRCData/$1"
fi

echo "Using $fsInputDir as input directory"

#delete local output file
rm -rf output
#create the new jar
jar cvfe IRCMerge.jar IRCMerge -C bin .
#delete previous output file
hadoop fs -rm -r IRCMergeOutput/output
#run map reduce
hadoop jar IRCMerge.jar $fsInputDir IRCMergeOutput/output
#copy output to local
hadoop fs -get IRCMergeOutput/output output
#open relevant file
gedit output/part-00000 &
