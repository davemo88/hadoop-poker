import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/*
 * amh564-dk2353:
 * IRCMerge is a map reduce program to clean and organize the data from the IRC Poker database.
 * The IRC database has several different file types: hand database (hdb), player database (pdb),
 * and roster database (rdb). These need to be merged to assess a chronological sequence of poker
 * actions. Then we extract feature vectors of the form:
 * <action, numPlayers, position, bank0, bank1, bank2, bank3, bank4, bank5, banke6, bank7, bank8, cards>
 * To meet our vector's requirements, we discard any data where the player's cards aren't shown, the player
 * isn't first to act, or the player in question doesn't win. 
 */
public class IRCMerge extends Configured implements Tool {

	@Override
	public int run(String[] args) throws Exception {
		if (args.length<2) {
			System.out.println("Usage: provide input and output directories.");
			return -1;
		}
		JobConf conf = new JobConf(IRCMerge.class);
		//necessary to pass subdirectories as input
		conf.setBoolean("mapreduce.input.fileinputformat.input.dir.recursive", true);

		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));
		
		conf.setMapperClass(IRCMergeMapper.class);
		conf.setReducerClass(IRCMergeReducer.class);
		
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
		
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);
		
		JobClient.runJob(conf);
		
		return 0;
	}
	
	public static void main(String args[]) throws Exception {
		
		System.out.println("Hello from IRCMerge main");
		
		int exitCode = ToolRunner.run(new IRCMerge(), args);
		System.exit(exitCode);
	}
}
