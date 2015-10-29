import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;


public class IRCMergeMapper extends MapReduceBase 
	implements Mapper<Text, Text, Text, Text> {

	@Override
	public void map(Text key, Text value,
			OutputCollector<Text, Text> output, Reporter r)
			throws IOException {

		String[] parsedInput = value.toString().split(" ");

		//determine the filetype
		String fileType;
		try {
			Integer.parseInt(parsedInput[0].trim());
			fileType = "DELIMhdb";
		} catch (NumberFormatException nfe) {
			fileType = "DELIMpdb";
		}
		
		String newKey = " ";
		String newValue = " ";
		if (fileType.equals("hdb")) {
			//key = handnum
			newKey = parsedInput[0].trim();
			StringBuilder sb = new StringBuilder(128);
			//value = file type and ... 
			sb.append("hdb ");
			//num players
			sb.append(parsedInput[3].trim());
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
			if (!parsedInput[11].equals("") && !parsedInput[12].equals("")) {
				//key = hand num
				newKey = parsedInput[1].trim();
				StringBuilder sb = new StringBuilder(128);
				//file type
				sb.append("pdb ");
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
			}
		} else {
			//an error occurred
			System.out.println("\n\nAH: Unrecognized file type!\n\n");
		}
		//output.collect(new Text(newKey), new Text(newValue));		
		output.collect(new Text("test"), new Text("test"));
	}
}
