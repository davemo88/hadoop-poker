import java.awt.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

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
		
		/* David's proposed feature vector is:
		 * --Chips in order around table, starting with player (meaning bankroll?)
		 *      --This will be extra work
		 * --Distance to dealer from player
		 *      --Same as position?
		 * --Binary flags for Fold, Call, Raise
		 *      --no prob
		 * --Amount raised or called
		 *      --I don't have this info. Only the total action of player during hand. 
		 * --Total bet
		 *      --I don't even have this! Only total for full hand. 
		 * --Pot after action
		 *      --I don't have this. 
		 * --Winnings
		 *      --no prob
		 * ?? Cards aren't part of the vector? 
		 */
		
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
		
		//general hand info
		String handNum = key.toString();
		String numPlayers = "";
		String sizeOfFlopPot = "";
		String communityCards = "";
		
		class PdbData {
			String nickname;
			String position;
			String startingBankroll;
			String preflopAction;
			String amountWon;
			String pocketCards;
		}
		
		ArrayList<PdbData> pdbFiles = new ArrayList<PdbData>();
		
		//for each value associated with this intermediate key
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
				numPlayers = parsedVector[0];
				sizeOfFlopPot = parsedVector[1];
				communityCards = parsedVector[2];
			} else if (fileType.equals("pdb")) {
				//for debugging: output.collect(new Text("pdb"), new Text(valueVector));
				String[] parsedVector = valueVector.split("\\s+");
				PdbData pdbData = new PdbData();
				pdbData.nickname = parsedVector[0];
				pdbData.position = parsedVector[1];
				pdbData.startingBankroll = parsedVector[2];
				pdbData.preflopAction = parsedVector[3];
				pdbData.amountWon = parsedVector[4];
				pdbData.pocketCards = parsedVector[5];
				pdbFiles.add(pdbData);
			} else {
				output.collect(new Text("??"), new Text("unrecognized filetype"));
			}
		}
		
		//create an output pair for each player of this hand
		for (int i = 0; i < pdbFiles.size(); i++) {
			PdbData pdbData = pdbFiles.get(i);
			//build output value
			StringBuilder sb = new StringBuilder(128);
			sb.append(handNum);
			sb.append(" ");
			sb.append(numPlayers);
			sb.append(" ");
			sb.append(sizeOfFlopPot);
			sb.append(" ");
			sb.append(communityCards);
			sb.append(" ");
			sb.append(pdbData.position);
			sb.append(" ");
			sb.append(pdbData.startingBankroll);
			sb.append(" ");
			sb.append(pdbData.preflopAction);
			sb.append(" ");
			sb.append(pdbData.amountWon);
			sb.append(" ");
			sb.append(pdbData.pocketCards);
			String newValue = sb.toString();
			output.collect(new Text(pdbData.nickname), new Text(newValue));
		}
	}
}
