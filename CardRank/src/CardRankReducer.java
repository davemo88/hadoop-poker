import java.awt.List;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;


public class CardRankReducer extends MapReduceBase 
	implements Reducer<Text, Text, Text, Text> {

	@Override
	public void reduce(Text key, Iterator<Text> values,
			OutputCollector<Text, Text> output, Reporter r)
			throws IOException {
		
		//output each vector without modification
		while (values.hasNext()) {
			String value = values.next().toString();
			output.collect(new Text(key), new Text(value));
		}
	}
}
