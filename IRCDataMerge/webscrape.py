#!/usr/bin/env python

import requests
import zlib

import urllib2
import StringIO
import gzip
import os
import zipfile
import tarfile
import glob
import shutil
import re

print "hi"

def downloadAndMoveFile(baseURL,filename):
	gametype = filename.split('.')[0]
	gamenum = filename.split('.')[1]

	#download the tgz file
	url = baseURL + "/" + filename
	response = requests.get(url)
	outfile = open(filename, "w")
	outfile.write(response.content)
	outfile.close()

	#extract the files 
	archivelist = glob.glob(filename)
	for x in archivelist:
		print x
		tar = tarfile.open(x)
		tar.extractall("temp")
		tar.close()
		#rename and move subdir
		dest = "scrapedData/%s-%s" % (gametype, gamenum)
		src = "temp/%s/%s" % (gametype, gamenum)
		shutil.rmtree(dest, ignore_errors=True)
		shutil.move(src, dest)
		shutil.rmtree("temp")

#read index page
baseURL = 'http://poker.cs.ualberta.ca/IRCdata/'
response = requests.get(baseURL)
index = response.content

#extract list of tarfiles to download
tarList = []
m = re.findall('href=\"((\w+).\d+.tgz)\"', index)
for match in m:
	gameType = match[1]
	if gameType == 'nolimit':
		tarName = match[0]	
		downloadAndMoveFile(baseURL, tarName)		
		os.remove(tarName)


