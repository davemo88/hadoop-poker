import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;


public class CardRankMapper extends MapReduceBase 
	implements Mapper<LongWritable, Text, Text, Text> {

	@Override
	public void map(LongWritable key, Text value,
			OutputCollector<Text, Text> output, Reporter r)
			throws IOException {

		//for debugging: output.collect(new Text("MapperInput: "), new Text("MapperInput: " + value.toString()));
		
		String[] parsedInput = value.toString().split("\\s+");
		final int cardsIndex = 6;
		
		//sanity check
		if (parsedInput.length < cardsIndex) {
			output.collect(new Text("problem"), new Text("card index is larger than num values"));
			return;
		}
		
		String bothCards = parsedInput[cardsIndex];
		String[] cards = bothCards.split(",");
		char card1Value = Character.toLowerCase(cards[0].charAt(0));
		char card1Suit = Character.toLowerCase(cards[0].charAt(1));
		char card2Value = Character.toLowerCase(cards[1].charAt(0));
		char card2Suit = Character.toLowerCase(cards[1].charAt(1));
		
		boolean suited;
		if (card1Suit == card2Suit) {
			suited = true;
		} else {
			suited = false;
		}
		
		//I think we should keep the string next to its value so we can compare. 
		//For example, I wonder how much the odds of winning match up with this table.
		
		//there could be one suited map and another non suited map
		//decide which map then enter the 2 values: 1,2 -- the table will have been entered with 1,2 and 2,1 mapping to the same value
		//Fill map:
		
		Map<String, Float> suitedMap = new HashMap<String, Float>();
		Map<String, Float> unsuitedMap = new HashMap<String, Float>();
		
		suitedMap.put("2,3", (float) 0.32);
		suitedMap.put("2,4", (float) 0.33);
		suitedMap.put("2,5", (float) 0.34);
		suitedMap.put("2,6", (float) 0.34);
		suitedMap.put("2,7", (float) 0.35);
		suitedMap.put("2,8", (float) 0.37);
		suitedMap.put("2,9", (float) 0.39);
		suitedMap.put("2,10",(float) 0.42);
		suitedMap.put("2,j", (float) 0.44);
		suitedMap.put("2,q", (float) 0.47);
		suitedMap.put("2,k", (float) 0.51);
		suitedMap.put("2,a", (float) 0.55);
		
		/*suitedMap.put("3,4", 1);
		suitedMap.put("3,5", 1);
		suitedMap.put("3,6", 1);
		suitedMap.put("3,7", 1);
		suitedMap.put("3,8", 1);
		suitedMap.put("3,9", 1);
		suitedMap.put("3,10", 1);
		suitedMap.put("3,j", 1);
		suitedMap.put("3,q", 1);
		suitedMap.put("3,k", 1);
		suitedMap.put("3,a", 1);
		
		suitedMap.put("4,5", 1);
		suitedMap.put("4,6", 1);
		suitedMap.put("4,7", 1);
		suitedMap.put("4,8", 1);
		suitedMap.put("4,9", 1);
		suitedMap.put("4,10", 1);
		suitedMap.put("4,j", 1);
		suitedMap.put("4,q", 1);
		suitedMap.put("4,k", 1);
		suitedMap.put("4,a", 1);

		suitedMap.put("5,6", 1);
		suitedMap.put("5,7", 1);
		suitedMap.put("5,8", 1);
		suitedMap.put("5,9", 1);
		suitedMap.put("5,10", 1);
		suitedMap.put("5,j", 1);
		suitedMap.put("5,q", 1);
		suitedMap.put("5,k", 1);
		suitedMap.put("5,a", 1);
		
		suitedMap.put("6,7", 1);
		suitedMap.put("6,8", 1);
		suitedMap.put("6,9", 1);
		suitedMap.put("6,10", 1);
		suitedMap.put("6,j", 1);
		suitedMap.put("6,q", 1);
		suitedMap.put("6,k", 1);
		suitedMap.put("6,a", 1);
		
		suitedMap.put("7,8", 1);
		suitedMap.put("7,9", 1);
		suitedMap.put("7,10", 1);
		suitedMap.put("7,j", 1);
		suitedMap.put("7,q", 1);
		suitedMap.put("7,k", 1);
		suitedMap.put("7,a", 1);
		
		suitedMap.put("8,9", 1);
		suitedMap.put("8,10", 1);
		suitedMap.put("8,j", 1);
		suitedMap.put("8,q", 1);
		suitedMap.put("8,k", 1);
		suitedMap.put("8,a", 1);
		
		suitedMap.put("9,10", 1);
		suitedMap.put("9,j", 1);
		suitedMap.put("9,q", 1);
		suitedMap.put("9,k", 1);
		suitedMap.put("9,a", 1);
		
		suitedMap.put("10,j", 1);
		suitedMap.put("10,q", 1);
		suitedMap.put("10,k", 1);
		suitedMap.put("10,a", 1);

		suitedMap.put("j,q", 1);
		suitedMap.put("j,k", 1);
		suitedMap.put("j,a", 1);
		
		suitedMap.put("q,k", 1);
		suitedMap.put("q,a", 1);		
		
		suitedMap.put("k,a", 1);*/
		
		//Suited table
		
		
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
		} 
		
		output.collect(new Text(newKey), new Text(newValue));		
	}
}
