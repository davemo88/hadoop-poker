import java.awt.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;


/*
 * amh564-dk2353:
 * IRCMerge is a map reduce program to clean and organize the data from the IRC Poker database.
 * The IRC database has several different file types: hand database (hdb), player database (pdb),
 * and roster database (rdb). These need to be merged to assess a chronological sequence of poker
 * actions. Then we extract feature vectors of the form:
 * <action, numPlayers, position, bank0, bank1, bank2, bank3, bank4, bank5, banke6, bank7, bank8, cards>
 * To meet our vector's requirements, we discard any data where the player's cards aren't shown, the player
 * isn't first to act, or the player in question doesn't win. 
 * 
 * The reducer receives all the extracted hdb and pdb data associated with a given handnum. Creating a feature
 * vector happens in two stages:
 * First it iterates through each value associated with a handum and constructs a list of hdbData and
 * pdbData structs.
 * Once all the structs for a given handnum have been aggregated, it iterates through them to creates a 
 * feature vector of the form:
 * <action, numPlayers, position, bank0, bank1, bank2, bank3, bank4, bank5, banke6, bank7, bank8, cards>
 * Each vector value is normalized. 
 */

public class IRCMergeReducer extends MapReduceBase 
	implements Reducer<Text, Text, Text, Text> {

	//general hand info
	class HdbData {
		String handNum;
		String numPlayers;
		String communityCards;
	}
	
	//player info
	class PdbData {
		String nickname;
		String position;
		String startingBankroll;
		String preflopActions;
		String amountWon;
		String pocketCards;
	}
	
	@Override
	public void reduce(Text key, Iterator<Text> values,
			OutputCollector<Text, Text> output, Reporter r)
			throws IOException {
		
		//global variables
		final int const_players = 9;
		int largestBankroll = 1;
		Map<Integer, PdbData> pdbMap = new HashMap<Integer, PdbData>();
		HdbData hdbData = new HdbData();
		
		//for each value associated with this intermediate key (hand num)
		while (values.hasNext()) {			
			String valueString = values.next().toString();
			//For debugging: output.collect(key, new Text(valueString));
			String[] valueParts = valueString.split("DELIM");
			if (valueParts.length != 2) {
				output.collect(new Text("No Delim in reducer input??"), new Text(valueString));
				continue;
			}
			String fileType = valueParts[0].trim();
			String valueVector = valueParts[1].trim();
			
			if (fileType.equals("hdb")) {
				//for debugging: output.collect(new Text("hdb"), new Text(valueVector));
				String[] parsedVector = valueVector.split("\\s+");
				hdbData.handNum = key.toString();
				hdbData.numPlayers = parsedVector[0];
				hdbData.communityCards = parsedVector[1];
			} else if (fileType.equals("pdb")) {
				//for debugging: output.collect(new Text("pdb"), new Text(valueVector));
				String[] parsedVector = valueVector.split("\\s+");
				PdbData pdbData = new PdbData();
				pdbData.nickname = parsedVector[0];
				int intPosition = Integer.parseInt(parsedVector[1]) - 1; //position range is 0-8 not 1-9
				pdbData.position = Integer.toString(intPosition);  
				pdbData.startingBankroll = parsedVector[2];
				pdbData.preflopActions = parsedVector[3];
				pdbData.amountWon = parsedVector[4];
				pdbData.pocketCards = parsedVector[5];
				pdbMap.put(intPosition, pdbData);
				//keep track of largest bankroll for normalization
				if (Integer.parseInt(pdbData.startingBankroll) > largestBankroll) {
					largestBankroll = Integer.parseInt(pdbData.startingBankroll);
				}
			} else {
				//output.collect(new Text("NEITHER HDBorPDB"), new Text(valueVector));
			}
		}
		
		//create an output pair for each player that has won
		parentloop:
		for (int i = 0; i < pdbMap.size(); i++) {
			PdbData pdbData = pdbMap.get(i);
			int numPlayers = Integer.parseInt(hdbData.numPlayers);
			
			//discard data if this player didn't win
			if (Integer.parseInt(pdbData.amountWon) <= 0) {
				continue;
			}
			//discard data if pocket cards aren't shown
			if (pdbData.pocketCards.equals("-,-")) {
				continue;
			}
	
			//build output value
			StringBuilder sb = new StringBuilder(128);
			//num players is divided by 9
			String normNumPlayers = normalizeString(hdbData.numPlayers, 9);
			sb.append(normNumPlayers);
			sb.append(" ");
			//position is divided by 8
			String normPosition = normalizeString(pdbData.position, 8);
			sb.append(normPosition);
			sb.append(" ");
			//all 9 bankrolls in order starting at position
			int startingPos = Integer.parseInt(pdbData.position);
			for (int j = 0; j < const_players; j++) {
				int bankrollPos = (startingPos + j) % const_players;
				if (pdbMap.containsKey(bankrollPos)) {
					//normalize by largest bankroll
					String normalizedBankroll = normalizeString(pdbMap.get(bankrollPos).startingBankroll, largestBankroll);
					sb.append(normalizedBankroll);
				} else {
					sb.append("0");
				}
				sb.append(" ");
			}
			//preflop action - discard if this player was not first to act
			String preflopActions = pdbData.preflopActions;
			int numRotations = preflopActions.length();
			char action = ' ';
			for (int pos = 0; pos < numPlayers; pos++) { 
				try {
					action = pdbMap.get(pos).preflopActions.charAt(0);
				} catch (StringIndexOutOfBoundsException e) {
					action = ' ';
				}
				if ((action == 'b') ||  //bet
					(action == 'c') ||  //call
					(action == 'r') ||  //raise
					(action == 'A')) {  //All in
					if (pos == i) { //proceed if current player was first to act
						break;
					} else {
						continue parentloop; //discard data if another player was first to act
					}
				} else {
					//if the current player performed a non-action action (ex Blind), discard data
					if (pos == i) {
						continue parentloop;
					}
				}
			}
			//convert first action to the key
			String newKey = "";
			if (action == 'f') {
				newKey = "folds";
			} else if (action == 'c') {
				newKey = "calls";
			} else if ((action == 'r') || (action == 'b')) {
				newKey = "raises";
			} else {
				newKey = "error?";
			}
			//pocket cards
			sb.append(pdbData.pocketCards);
			String newValue = sb.toString();
			//String keyString = hdbData.handNum + "-" + pdbData.nickname;
			//output.collect(new Text(keyString), new Text(newValue));
			output.collect(new Text(newKey), new Text(newValue));
		}
	}
	
	public String normalizeString(String denorm, float maxVal) {		
		int denorm_int = Integer.parseInt(denorm);
		float normalized = denorm_int / maxVal;
		return String.format("%.3f", normalized);
	}
}
