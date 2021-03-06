#!/usr/bin/env python

import sys

"""
After the features have been extracted from both the PokerStars and IRC
hand histories, we need to convert the string representation of the player's
hand into a numeric representation. To do this we use a table which gives the
chance that a hand wins against a random hand. The table is below.
"""

odds = \
{'22': '0.503',
 '32o': '0.312',
 '32s': '0.351',
 '33': '0.537',
 '42o': '0.325',
 '42s': '0.363',
 '43o': '0.344',
 '43s': '0.38',
 '44': '0.57',
 '52o': '0.339',
 '52s': '0.375',
 '53o': '0.358',
 '53s': '0.393',
 '54o': '0.379',
 '54s': '0.411',
 '55': '0.603',
 '62o': '0.34',
 '62s': '0.375',
 '63o': '0.359',
 '63s': '0.394',
 '64o': '0.38',
 '64s': '0.414',
 '65o': '0.401',
 '65s': '0.432',
 '66': '0.633',
 '72o': '0.346',
 '72s': '0.381',
 '73o': '0.366',
 '73s': '0.4',
 '74o': '0.386',
 '74s': '0.418',
 '75o': '0.408',
 '75s': '0.438',
 '76o': '0.427',
 '76s': '0.457',
 '77': '0.662',
 '82o': '0.368',
 '82s': '0.403',
 '83o': '0.375',
 '83s': '0.408',
 '84o': '0.396',
 '84s': '0.427',
 '85o': '0.417',
 '85s': '0.448',
 '86o': '0.436',
 '86s': '0.465',
 '87o': '0.455',
 '87s': '0.482',
 '88': '0.691',
 '92o': '0.389',
 '92s': '0.423',
 '93o': '0.399',
 '93s': '0.432',
 '94o': '0.407',
 '94s': '0.438',
 '95o': '0.429',
 '95s': '0.459',
 '96o': '0.449',
 '96s': '0.477',
 '97o': '0.467',
 '97s': '0.495',
 '98o': '0.484',
 '98s': '0.511',
 '99': '0.721',
 'A2o': '0.546',
 'A2s': '0.57',
 'A3o': '0.556',
 'A3s': '0.58',
 'A4o': '0.564',
 'A4s': '0.589',
 'A5o': '0.577',
 'A5s': '0.599',
 'A6o': '0.578',
 'A6s': '0.6',
 'A7o': '0.591',
 'A7s': '0.611',
 'A8o': '0.601',
 'A8s': '0.621',
 'A9o': '0.609',
 'A9s': '0.63',
 'AA': '0.853',
 'AJo': '0.636',
 'AJs': '0.654',
 'AKo': '0.654',
 'AQo': '0.645',
 'AQs': '0.661',
 'ATo': '0.629',
 'ATs': '0.647',
 'AKs': '0.67',
 'J2o': '0.44',
 'J2s': '0.471',
 'J3o': '0.45',
 'J3s': '0.479',
 'J4o': '0.461',
 'J4s': '0.49',
 'J5o': '0.471',
 'J5s': '0.5',
 'J6o': '0.479',
 'J6s': '0.508',
 'J7o': '0.499',
 'J7s': '0.524',
 'J8o': '0.517',
 'J8s': '0.542',
 'J9o': '0.534',
 'J9s': '0.558',
 'JJ': '0.775',
 'JTo': '0.554',
 'JTs': '0.575',
 'K2o': '0.502',
 'K2s': '0.529',
 'K3o': '0.512',
 'K3s': '0.538',
 'K4o': '0.521',
 'K4s': '0.547',
 'K5o': '0.533',
 'K5s': '0.558',
 'K6o': '0.543',
 'K6s': '0.568',
 'K7o': '0.554',
 'K7s': '0.578',
 'K8o': '0.563',
 'K8s': '0.585',
 'K9o': '0.58',
 'K9s': '0.6',
 'KJo': '0.606',
 'KJs': '0.626',
 'KK': '0.824',
 'KQo': '0.614',
 'KQs': '0.634',
 'KTo': '0.599',
 'KTs': '0.619',
 'Q2o': '0.47',
 'Q2s': '0.499',
 'Q3o': '0.479',
 'Q3s': '0.507',
 'Q4o': '0.49',
 'Q4s': '0.517',
 'Q5o': '0.502',
 'Q5s': '0.529',
 'Q6o': '0.511',
 'Q6s': '0.538',
 'Q7o': '0.519',
 'Q7s': '0.545',
 'Q8o': '0.538',
 'Q8s': '0.562',
 'Q9o': '0.555',
 'Q9s': '0.579',
 'QJo': '0.582',
 'QJs': '0.603',
 'QQ': '0.799',
 'QTo': '0.574',
 'QTs': '0.595',
 'T2o': '0.415',
 'T2s': '0.447',
 'T3o': '0.424',
 'T3s': '0.455',
 'T4o': '0.434',
 'T4s': '0.464',
 'T5o': '0.442',
 'T5s': '0.472',
 'T6o': '0.463',
 'T6s': '0.492',
 'T7o': '0.482',
 'T7s': '0.51',
 'T8o': '0.5',
 'T8s': '0.526',
 'T9o': '0.517',
 'T9s': '0.543',
 'TT': '0.751'}

def process_hole_cards(hole_cards):
    """
    This function looks up a hand in the hand strenght table.
    A little formatting is required since the hands aren't 
    represented consistently. 
    """

    c1_rank = hole_cards[0][0]
    c1_suit = hole_cards[0][1]
    c2_rank = hole_cards[-1][0]
    c2_suit = hole_cards[-1][1]

    if c1_rank == c2_rank:
        hand = '%s%s' % (c1_rank, c2_rank)
    elif c1_suit == c2_suit:
        hand = '%s%ss' % (c1_rank, c2_rank)
    else:
        hand = '%s%so' % (c1_rank, c2_rank)

    reversed_hand = hand[:-1][::-1]+hand[-1]

    if hand in odds:
        hand_odds = odds[hand]
    elif reversed_hand in odds:
        hand_odds = odds[reversed_hand]
    return hand_odds

for line in sys.stdin:

    key, val = line.split('\t')

    val = val.split()

    val[-1] = process_hole_cards(val[-1].split(','))

    val = ' '.join(map(str, val))

    print '%s\t%s' % (key, val)