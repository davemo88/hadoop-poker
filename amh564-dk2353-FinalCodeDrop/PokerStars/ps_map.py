#!/usr/bin/env python

"""
The purpose of this file is to extract features from our PokerStars
hand history. These are hands from the perspective of a single player.
We only want hands where the player has the opportunity to be first to bet.
This is explained further in our paper but it is to simplify our analysis.

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

## this is the name of the player whose hand history we have
## and the only player whose cards we can see
PLAYER = 'jaimestaples'

## use this to 
hand_id = None

for line in sys.stdin:

## data looks like example above
## have to go through multiple rows for each record we emit
## we use hand_id to keep track of where we are
    if "Hand #" in line:

        hand_id = re.search('\d+',line).group()

        seat_chips = {}
        for i in range(1,10):
            seat_chips[i] = 0

## keep track of the player's cards and the action they take
        hole_cards = None
        action = None

## we only want data from hands where the player can be first to act,
## i.e. no other player has bet before it is PLAYER's turn to act
        first_to_act = False

## hand_id is not None if we are looking at a valid hand
    if hand_id:

## get where the button is. this will tell us our relative position
        if "is the button" in line:
            button = int(re.search('Seat #\d',line).group()[-1])

## get the number of chips each player has
        elif 'Seat' in line and 'chips' in line:
            nums = re.findall('\d+',line)
            seat_chips[int(nums[0])] = int(nums[-1])

## get PLAYER's seat
            if PLAYER in line:
                player_seat = int(nums[0])

## ignore hands where PLAYER is in the blinds
        elif 'posts' in line and 'blind' in line:
            if PLAYER in line:
## when hand_id is none we'll ignore rows until we get a new hand_id
                hand_id = None

        elif 'Dealt to %s' % (PLAYER) in line:
            hole_cards = line.split('[')[-1][:-2].split()

## ignore hand where somebody raises or calls before PLAYER gets to act
        elif ('raises' in line or 'calls' in line) \
        and PLAYER not in line and not first_to_act:
            hand_id = None

## these are the cases where PLAYER is first to act
        elif PLAYER in line and re.search('raises|calls|folds',line):
            first_to_act = True
            action = re.search('raises|calls|folds',line).group()

            if action == 'folds':

## fold is 0, other actions are 1
                action = 0

            elif action in ['raises', 'calls']:

                action = 1

## now we assemble the features for the hand, starting the with
## chips each player has
            chips = seat_chips.values()

## players in order around table starting with PLAYER
            val = chips[player_seat-1:]+chips[:player_seat-1]
## normalize to biggest stack size
            z = max(chips)
            val = [float(v)/z for v in val]
## add postion relative to the button
            val.insert(0, float((player_seat - button)) % 9 / 8.0)
## add total number of players
            val.insert(0, len(filter(lambda _ : _ != 0, chips)) / 9.0)
## add string representation of hole cards
## this will be changed to numerical representation
## in a successive map-reduce
            val.append(','.join(map(str,hole_cards)))

## change from list to string to print
            val = ' '.join(map(str, val))

## emit
            print '%s\t%s' % (action, val)

## set hand_id to None to ignore rows until we get to the next hand
            hand_id = None