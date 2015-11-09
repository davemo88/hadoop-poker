import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;


public class IRCMergeMapper extends MapReduceBase 
	implements Mapper<LongWritable, Text, Text, Text> {

	@Override
	public void map(LongWritable key, Text value,
			OutputCollector<Text, Text> output, Reporter r)
			throws IOException {

		//output the unmodified input for debugging purposes
		String[] parsedInput = value.toString().split("\\s+");
		int lenParse = parsedInput.length;
		//for debugging
		//output.collect(new Text("MapperInput: "), new Text("MapperInput: " + value.toString()));
		
		//determine the filetype. hdb starts with an int, pdb doesn't. 
		String fileType;
		try {
			Integer.parseInt(parsedInput[0].trim());
			fileType = "hdb";
		} catch (NumberFormatException nfe) {
			fileType = "pdb";
		}
		
		String newKey = " ";
		String newValue = " ";
		if (fileType.equals("hdb")) {
			if (lenParse < 13) {
				//board cards not revealed. discard data. 
				return;
			}
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
			//size of pot at flop
			String potSize = parsedInput[4].split("/")[1];
			sb.append(potSize);
			sb.append(" ");
			newValue = sb.toString();
			//community cards
			sb.append(parsedInput[8].trim());
			sb.append(",");
			sb.append(parsedInput[9].trim());
			sb.append(",");
			sb.append(parsedInput[10].trim());
			sb.append(",");
			sb.append(parsedInput[11].trim());
			sb.append(",");
			sb.append(parsedInput[12].trim());
			newValue = sb.toString();
		} else if (fileType.equals("pdb")) {
			//only proceed if pocket cards are revealed 
			if (lenParse < 13) {
				return;
			}
			/* intermediate value will be of the format:
			 * key:   {handNum}
			 * value: {hdbDELIM nickname position preflopaction amountwon card1,card2} 
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
			//preflop action
			sb.append(parsedInput[4]);
			sb.append(" ");
			//amount won
			sb.append(parsedInput[10]);
			sb.append(" ");
			//pocket cards
			sb.append(parsedInput[11]);
			sb.append(",");
			sb.append(parsedInput[12]);
			newValue = sb.toString();
		} else {
			//an error occurred
			output.collect(new Text("Mapper input: "), new Text("Mapper Input: unknown filetype"));
		}
		output.collect(new Text(newKey), new Text(newValue));		
	}
}
