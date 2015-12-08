import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
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
 * The Mapper is responsible for extracting the necessary information from either the hdb or pdb
 * files and associating it with it's hand number. 
 * From hdb it extracts: hand number, numPlayers, and community cards. 
 * From pdb it extracts: nickname, position, pocket cards, and bankroll.  
 * The Reducer will receive all these values from each hdb and pdb associated with a given handnum
 * and will create the feature vector.
 */

public class IRCMergeMapper extends MapReduceBase 
	implements Mapper<LongWritable, Text, Text, Text> {

	@Override
	public void map(LongWritable key, Text value,
			OutputCollector<Text, Text> output, Reporter r)
			throws IOException {

		String[] parsedInput = value.toString().split("\\s+");
		int lenParse = parsedInput.length;
		//for debugging: output.collect(new Text("MapperInput: "), new Text("MapperInput: " + value.toString()));
		
		//determine the filetype.
		//hdb starts with an int. pdb doesn't
		//rdb has an int at its 0th and 2nd index. 
		String fileType;
		try {
			Integer.parseInt(parsedInput[0].trim());
			try {
				Integer.parseInt(parsedInput[2].trim());
				fileType = "hdb";
			} catch (NumberFormatException nfe) {
				fileType = "rdb";
			}
		} catch (NumberFormatException nfe) {
			fileType = "pdb";
		}
		
		String newKey = " ";
		String newValue = " ";
		if (fileType.equals("hdb")) {
			/* intermediate value will be of the format:
			 * key:   {handNum}
			 * value: {hdbDELIM numPlayers sizeOfPotAtFlop card1,card2,card3,card4,card5} 
			 */
			//key = handnum
			newKey = parsedInput[0];
			//construct value
			StringBuilder sb = new StringBuilder(256); 
			sb.append("hdbDELIM ");
			//num players
			sb.append(parsedInput[3]);
			sb.append(" ");
			//community cards
			if (lenParse < 13) {
				//community cards not revealed. use placeholders
				sb.append("-,-,-,-,-");
			} else {
				sb.append(parsedInput[8].trim());
				sb.append(",");
				sb.append(parsedInput[9].trim());
				sb.append(",");
				sb.append(parsedInput[10].trim());
				sb.append(",");
				sb.append(parsedInput[11].trim());
				sb.append(",");
				sb.append(parsedInput[12].trim());
			}
			newValue = sb.toString();
		} else if (fileType.equals("pdb")) {
			/* intermediate value will be of the format:
			 * key:   {handNum}
			 * value: {hdbDELIM nickname position startingBankroll preflopaction amountwon card1,card2} 
			 */
			newKey = parsedInput[1].trim();
			StringBuilder sb = new StringBuilder(128);
			//file type
			sb.append("pdbDELIM ");
			//nickname
			sb.append(parsedInput[0]);
			sb.append(" ");
			//position
			sb.append(parsedInput[3]);
			sb.append(" ");
			//starting bankroll
			sb.append(parsedInput[8]);
			sb.append(" ");
			//preflop action
			sb.append(parsedInput[4]);
			sb.append(" ");
			//amount won
			sb.append(parsedInput[10]);
			sb.append(" ");
			//pocket cards
			if (lenParse < 13) {
				//not shown. use placeholders.
				sb.append("-,-");
			} else {
				sb.append(parsedInput[11]);
				sb.append(",");
				sb.append(parsedInput[12]);
			}
			newValue = sb.toString();
		} else if (fileType == "rdb"){
			//rdb is discarded
			return;
		} else {
			//error?
			//output.collect(new Text("Mapper input: "), new Text("Mapper Input: unknown filetype"));
			return;
		}
		output.collect(new Text(newKey), new Text(newValue));		
	}
}
