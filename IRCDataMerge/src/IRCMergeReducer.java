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

	@Override
	public void reduce(Text key, Iterator<Text> values,
			OutputCollector<Text, Text> output, Reporter r)
			throws IOException {
		
		/* IRC action notation:
		-       no action; player is no longer contesting pot
        B       blind bet
        f       fold
        k       check
        b       bet
        c       call
        r       raise
        A       all-in
        Q       quits game
        K       kicked from game*/
		
		/* Output will be of the format:
		 * Key = handnum
		 * Value = num players, 9bankrolls in order starting with player, position, 
		 * 3preflop flags, amount won, pocket cards
		 */
		
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
			String preflopAction;
			String amountWon;
			String pocketCards;
		}
		
		//global variables
		final int const_players = 9;
		int largestBankroll = 1;
		Map<Integer, PdbData> pdbMap = new HashMap<Integer, PdbData>();
		ArrayList<PdbData> pdbFiles = new ArrayList<PdbData>();
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
				pdbData.preflopAction = parsedVector[3];
				pdbData.amountWon = parsedVector[4];
				pdbData.pocketCards = parsedVector[5];
				pdbFiles.add(pdbData);
				pdbMap.put(intPosition, pdbData);
				//keep track of largest bankroll for normalization
				if (Integer.parseInt(pdbData.startingBankroll) > largestBankroll) {
					largestBankroll = Integer.parseInt(pdbData.startingBankroll);
				}
			} else {
				output.collect(new Text("??"), new Text("unrecognized filetype"));
			}
		}
		
		//create an output pair for each player that has won
		for (int i = 0; i < pdbFiles.size(); i++) {
			PdbData pdbData = pdbFiles.get(i);
			//discard data if this player didn't win
			if (Integer.parseInt(pdbData.amountWon) <= 0) {
				return;
			}
			//build output value
			StringBuilder sb = new StringBuilder(128);
			sb.append(hdbData.numPlayers);
			sb.append(" ");
			sb.append(pdbData.position);
			sb.append(" ");
			//all 9 bankrolls in order starting at position
			int startingPos = Integer.parseInt(pdbData.position);
			for (int j = 0; j < const_players; j++) {
				int bankrollPos = (startingPos + j) % const_players;
				if (pdbMap.containsKey(bankrollPos)) {
					String bankroll = pdbMap.get(bankrollPos).startingBankroll;
					float normalizedBankroll = Integer.parseInt(bankroll) / (float)largestBankroll;
					sb.append(Float.toString(normalizedBankroll));
					//not normalized: sb.append(bankroll);
				} else {
					sb.append("-");
				}
				if (j != const_players-1) {
					sb.append(",");
				} else {
					sb.append(" ");
				}
			}
			//preflop action - requires parsing into flags: blind, fold, check, call, raise, other
			String action = pdbData.preflopAction;
			if (action.equals("B")) {
				//blind bet
				sb.append("1,0,0,0,0,0 ");
			} else if (action.equals("f")) {
				//fold
				sb.append("0,1,0,0,0,0 ");
			} else if (action.equals("k")) {
				//check
				sb.append("0,0,1,0,0,0 ");
			} else if (action.equals("c")) {
				//call
				sb.append("0,0,0,1,0,0 ");
			} else if (action.equals("r")) {
				//raise
				sb.append("0,0,0,0,1,0 ");
			} else if (action.equals("A")) {
				//all in - I'm treating this as a raise
				sb.append("0,0,0,0,1,0 ");
			} else {
				//other = b (bet), Q(quits game), K (kicked from game), or incorrect syntax
				sb.append("0,0,0,0,0,");
				sb.append(action);
				sb.append(" ");
			}	
			//amount won
			sb.append(pdbData.amountWon);
			sb.append(" ");
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
			output.collect(new Text(keyString), new Text(newValue));
		}
	}
}
