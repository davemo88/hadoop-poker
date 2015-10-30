"""
code to grab poker hands from boomplayer.com

needs to be adapted for map reduce

"""



import requests
import re
from xml.etree import ElementTree

"""

feature for every game action like hbase. giant nosql tables

find a way to compare decisions like a distance function e.g. for each decision in a hand, we count aggregate results of similar hands

first dot product all prefixes of length one, then all hands of length 2 that are similar enough in prefix. do this recursively

when computing distance, since prefix distance is low, only need to compute distance for this decision

index all decisions i.e. sort in a bunch of dimensions?

calculate ev for every action in data set for a given hand prefix mixture (similar hand prefixes) e.g. hands in similar range, stack sizes, pot sizes, position

we only do hand prefixes in the data and we calculate each prefix only once so it shouldn't be so bad

should store length of hand

"""

class Boomplayer(object):


    def get_pCodePath(self, source):
        """
        get the pCodePath given the source code of a boomplayer replay

        """

        s = re.search('pCodePath=http://www.boomplayer.com/repository/pCode/[A-Z0-9_]+/[A-Z0-9_]+/[A-Z0-9_]+.xml', source)

        if s:

            return s.group()[len('pCodePath='):]

    def get_pCode(self, boomplayer_url):
        """ 
        get the raw pcode xml given the url of a boomplayer replay

        """

        source = requests.get(boomplayer_url).content

        pCodePath = self.get_pCodePath(source)

        if pCodePath:

            return requests.get(pCodePath).content

    def parse_pCode(self, pCode):
        """
        load the raw pCode xml into an ElementTree data structure

        then we would probably do some processing to extract data

        """

        root = ElementTree.fromstring(pCode)

        return root

        # game_info = root.find('j')




if __name__ == "__main__":

    b = Boomplayer()

    pCode = b.get_pCode('http://www.boomplayer.com/poker-hands/Boom/16226966_600A963870')

    et = b.parse_pCode(pCode)

    # .
    # .
    # .
    # .



