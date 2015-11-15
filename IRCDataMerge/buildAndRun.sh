#!/bin/bash

#delete local output file
rm -rf output
#create the new jar
jar cvfe IRCMerge.jar IRCMerge -C bin .
#delete previous output file
hadoop fs -rm -r IRCMergeOutput/output
#run map reduce
#hadoop jar IRCMerge.jar IRCMergeInput/199504Input IRCMergeOutput/output
hadoop jar IRCMerge.jar IRCData/ IRCMergeOutput/output
#copy output to local
hadoop fs -get IRCMergeOutput/output output
#open relevant file
gedit output/part-00000 &
