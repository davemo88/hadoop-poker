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
		
		/* IRC action notation:
		-       no action; player is no longer contesting pot
        B       blind bet
        f       fold
        = k       check
        = b       bet
        = c       call
        = r       raise
        = A       all-in
        Q       quits game
        K       kicked from game*/
		
		//global variables
		final int const_players = 9;
		int largestBankroll = 1;
		Map<Integer, PdbData> pdbMap = new HashMap<Integer, PdbData>();
		HdbData hdbData = new HdbData();
		
		//for each value associated with this intermediate key (hand num)
		while (values.hasNext()) {			
			String valueString = values.next().toString();
			//For debugging: output.collect(new Text("reducer input: "), new Text(valueString));
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
				output.collect(new Text("NEITHER HDBorPDB"), new Text(valueVector));
			}
		}
		
		//create an output pair for each player that has won
		for (int i = 0; i < pdbMap.size(); i++) {
			PdbData pdbData = pdbMap.get(i);
			int numPlayers = Integer.parseInt(hdbData.numPlayers);
			
			//discard data if this player didn't win
			if (Integer.parseInt(pdbData.amountWon) <= 0) {
				return;
			}
			//build output value
			StringBuilder sb = new StringBuilder(128);
			//num players is divided by 9
			String normNumPlayers = normalizeString(hdbData.numPlayers, 9);
			//sb.append(hdbData.numPlayers);
			sb.append(normNumPlayers);
			sb.append(" ");
			//position is divided by 8
			String normPosition = normalizeString(pdbData.position, 8);
			sb.append(normPosition);
			//sb.append(pdbData.position);
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
			outerloop:
			for (int rot = 0; rot < numRotations; rot++) {
				for (int p = 0; p < numPlayers; p++) { //this should change? to position?
					try {
						action = pdbMap.get(p).preflopActions.charAt(rot);
					} catch (StringIndexOutOfBoundsException e) {
						action = ' ';
					}
					if ((action == 'k') ||  //check
						(action == 'b') ||  //bet
						(action == 'c') ||  //call
						(action == 'r') ||  //raise
						(action == 'A')) {  //All in
						if (p == i) { //proceed if current player was first to act
							break outerloop;
						} else {
							return; //discard data if another player was first to act
						}
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
			//sb.append(newKey);
			//sb.append(" ");
			//pocket cards
			if (pdbData.pocketCards.equals("-,-")) {
				//discard data if pocket cards aren't shown
				return;
			} else {
				sb.append(pdbData.pocketCards);
				String newValue = sb.toString();
			}
			String keyString = hdbData.handNum + "-" + pdbData.nickname;
			String newValue = sb.toString();
			output.collect(new Text(newKey), new Text(newValue));
			//output.collect(new Text(keyString), new Text(newValue));
		}
	}
	
	public String normalizeString(String denorm, float maxVal) {		
		int denorm_int = Integer.parseInt(denorm);
		float normalized = denorm_int / maxVal;
		return String.format("%.3f", normalized);
	}
}
