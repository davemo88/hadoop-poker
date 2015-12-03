#!/usr/bin/env python

"""
example hand:

PokerStars Home Game Hand #127873232695: {Staples Poker League} Tournament #1082114955, $1.00+$0.10 USD Hold'em No Limit - Level II (20/40) - 2015/01/01 15:05:50 MT [2015/01/01 17:05:50 ET]
Table '1082114955 3' 9-max Seat #6 is the button
Seat 1: Flowville15 (2045 in chips) 
Seat 2: 3088shane (1625 in chips) 
Seat 4: jaimestaples (1955 in chips) 
Seat 5: Huds0nat0r (1730 in chips) 
Seat 6: stedesB (2645 in chips) 
Seat 7: semdion75 (1970 in chips) 
Seat 8: Frederus (2000 in chips) 
Seat 9: Oimelschlonz (1910 in chips) 
semdion75: posts small blind 20
Frederus: posts big blind 40
*** HOLE CARDS ***
Dealt to jaimestaples [9s Js]
Oimelschlonz: folds 
Flowville15: folds 
3088shane: folds 
jaimestaples: raises 60 to 100
Huds0nat0r: folds 
stedesB: folds 
semdion75: folds 
Frederus: folds 
Uncalled bet (60) returned to jaimestaples
jaimestaples collected 100 from pot
jaimestaples: doesn't show hand 
*** SUMMARY ***
Total pot 100 | Rake 0 
Seat 1: Flowville15 folded before Flop (didn't bet)
Seat 2: 3088shane folded before Flop (didn't bet)
Seat 4: jaimestaples collected (100)
Seat 5: Huds0nat0r folded before Flop (didn't bet)
Seat 6: stedesB (button) folded before Flop (didn't bet)
Seat 7: semdion75 (small blind) folded before Flop
Seat 8: Frederus (big blind) folded before Flop
Seat 9: Oimelschlonz folded before Flop (didn't bet)
"""

import sys
import re

## this could be a command line arg
PLAYER = 'jaimestaples'

key = None

## here there be bugs
for line in sys.stdin:

    if "Hand #" in line:

        key = re.search('\d+',line).group()

        seat_chips = {}
        for i in range(1,10):
            seat_chips[i] = 0

        hole_cards = None
        action = None
        amount = None
        total_bet = None
        pot = 0
        winnings = 0

## do i need this?
        postflop = False
        first_to_act = False

    if key and not postflop:

        if "is the button" in line:
            button = int(re.search('Seat #\d',line).group()[-1])

        elif 'Seat' in line and 'chips' in line:
            nums = re.findall('\d+',line)
            seat_chips[int(nums[0])] = int(nums[-1])

            if PLAYER in line:
## need to add relative position like distance from button
                player_stack = nums[-1]
                player_pos = int(nums[0])

## ignore hands where PLAYER is in the blinds
        elif 'posts' in line and 'blind' in line:
            if PLAYER in line:
                key = None
            else:
                pot += int(re.findall('\d+', line)[-1])

        elif 'Dealt to %s' % (PLAYER) in line:
            hole_cards = line.split('[')[-1][:-2].split()

## ignore hand where somebody raises or calls before PLAYER gets to act
        elif ('raises' in line or 'calls' in line) \
        and PLAYER not in line and not first_to_act:
            key = None

        elif PLAYER in line and re.search('raises|calls|folds',line):
            first_to_act = True
            action = re.search('raises|calls|folds',line).group()

            if action == 'folds':

                amount = 0
                total_bet = 0

            elif action == 'raises':

                amount = int(re.findall('\d+', line)[-2])
                total_bet = int(re.findall('\d+', line)[-1])

            elif action == 'calls':

                amount = int(re.findall('\d+', line)[-1])
                total_bet = amount

        elif 'collected' in line:

            if PLAYER in line:

                winnings = int(re.findall('\d+', line)[-1])

## when we know the payoff we can emit the key with our feature vector
## values are:
## num players
## position from button
## chips in order around the table starting with player, 
## hold cards like Ay,Bx
            chips = seat_chips.values()
## normalize to biggest stack size
            z = max(chips)

            val = chips[player_pos-1:]+chips[:player_pos-1]

            val = [float(v)/z for v in val]

            val.insert(0, float((button - player_pos) % 9) / 8.0)
            val.insert(0, len(filter(lambda _ : _ != 0, chips)) / 9.0)

## how far from the button
            val.append(','.join(map(str,hole_cards)))

            val = ' '.join(map(str, val))

            print '%s\t%s' % (action, val)

## set key to none until we get to the next hand
            key = None