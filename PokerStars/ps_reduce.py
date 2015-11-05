#!/usr/bin/env python
import sys

for line in sys.stdin:

    k,v = line.split('\t')

    print '%s\t%s' % (k,v)