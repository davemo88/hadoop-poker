import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class CardRank extends Configured implements Tool {

	@Override
	public int run(String[] args) throws Exception {
		if (args.length<2) {
			System.out.println("Usage: provide input and output directories.");
			return -1;
		}
		JobConf conf = new JobConf(CardRank.class);
		
		FileInputFormat.setInputPaths(conf, new Path(args[0]));
		FileOutputFormat.setOutputPath(conf, new Path(args[1]));
		
		conf.setMapperClass(CardRankMapper.class);
		conf.setReducerClass(CardRankReducer.class);
		
		conf.setMapOutputKeyClass(Text.class);
		conf.setMapOutputValueClass(Text.class);
		
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(Text.class);
		
		JobClient.runJob(conf);
		
		return 0;
	}
	
	public static void main(String args[]) throws Exception {
		
		System.out.println("Hello from IRCMerge main");
		
		int exitCode = ToolRunner.run(new CardRank(), args);
		System.exit(exitCode);
	}
}
