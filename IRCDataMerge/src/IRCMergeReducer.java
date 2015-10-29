import java.io.IOException;
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
		
		String[] inputVectors = values.toString().split("DELIM");
		int numVectors = inputVectors.length;
		
		//find the hdb vector.
		String hdbVector = "";
		for (int i = 0; i < numVectors; i++) {
			String[] parsedVector = inputVectors[i].split(" ");
			if (parsedVector[0].equals("hdb")) {
				hdbVector = inputVectors[i];
			} 
		}
		
		//create the hybrid vector
		String hybridVector;
		for (int i = 0; i < numVectors; i ++) {
			String[] parsedVector = inputVectors[i].split(" ");
			if (parsedVector[0].equals("pdb")) {
				StringBuilder sb = new StringBuilder(128);
				sb.append(hdbVector);
				sb.append(" ");
				sb.append(inputVectors[i]);
				hybridVector = sb.toString();
				output.collect(new Text("anything"), new Text(hybridVector));
			}
		}
	}
}
