from pyspark.mllib.classification import SVMWithSGD, SVMModel
from pyspark.mllib.regression import LabeledPoint

from pyspark import SparkContext

from csv import writer

from numpy.polynomial import polynomial as npp

## can't run the mllib using yarn on dumbo yet
#sc = SparkContext("yarn-client", "some_app")
## so we just run it with the local spark on the login node :(
sc = SparkContext("local", "some_app")
log_data = sc.textFile("spark.log").cache()

## split a record into the label and the associated features
def get_labeled_point(line):
    items = line.strip().split()
    y = items[0]
    x = items[1:]
    # return LabeledPoint(y, x)
## this explicitly maps each example to a higher dimensional space
## namely the space of a degree 2 polynomial kernel
    poly = npp.Polynomial([float(_) for _ in x])
    return LabeledPoint(y, (poly*poly).coef)

## load data and prep for SVM
data = sc.textFile("all_hands.txt")
examples = sc.parallelize(data.map(get_labeled_point).collect())

results = {}

## train SVMs with different regularization parameters
for exponent in range(5,11,2):
    model = SVMWithSGD.train(examples,
                             iterations=50,
                             regParam=2 ** exponent,
                             miniBatchFraction=1,
                             step=1)

## compute training error for that regParam
    incorrect_predictions = examples.map(lambda p: p.label != model.predict(p.features))
    training_error = incorrect_predictions.filter(lambda p : p).count() / float(examples.count())
    print "Training Error: %s" % training_error

    results[2**exponent] = training_error

## we could save these to a file
print results