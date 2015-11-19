#!/usr/bin/env python
"""
code to run many svms in parallel using libsvm

"""



from sys import path, stdin
## what to do for mapreduce?
path.insert(0,'/Users/davemo88/libsvm-320/python/')
from svmutil import *
import numpy as np

y,x = [], []

for line in stdin:

    key, val = line.split('\t')

    y.append(key)
    x.append(val)

sp = svm_problem(x,y)

polynomial_results = {}

for k in range(-5,6):
            C = 5 ** k
            row = [C]
            for d in range(1,6,2):
                print 'training with polynomial kernel degree {} and C = 5^{}'.format(d,k)
                polynomial_results[(d,C)] = svm_train(y,x,'-q -v 10 -t 1 -d {} -c {}'.format(d, C))

best_d, best_C = max(polynomial_results.keys(), key=lambda k : polynomial_results[k])

model = svm_train(y,x,'-q -t 1 -d {} -c {}').format(best_d, best_C)

for support_vector in model.get_SV():

    print '%s\t%s' % (support_vector[0], support_vector[1:])